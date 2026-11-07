package com.test.agent.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Agent 侧的协议适配器，不持有 JDBC 或数据库连接。 */
public class ConversionMcpClient implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ConversionMcpClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final McpSyncClient client;

    public ConversionMcpClient(ServerParameters parameters, Duration timeout) {
        var transport = new StdioClientTransport(parameters, new JacksonMcpJsonMapper(mapper)) {
            @Override
            protected ProcessBuilder getProcessBuilder() {
                ProcessBuilder builder = new ProcessBuilder();
                // SDK 默认 ProcessBuilder 会继承全部环境；仅保留 ServerParameters 中的白名单。
                builder.environment().clear();
                return builder;
            }
        };
        transport.setStdErrorHandler(line -> log.info("MCP server stderr: {}", line));
        client = McpClient.sync(transport).requestTimeout(timeout).build();
        try {
            client.initialize();
        } catch (RuntimeException e) {
            log.error("MCP 子进程初始化失败", e);
            if (!client.closeGracefully()) {
                e.addSuppressed(new IllegalStateException("MCP 初始化失败后的关闭未正常完成"));
            }
            throw new IllegalStateException("MCP 初始化失败，请检查子进程 stderr", e);
        }
    }

    ConversionMcpClient(McpSyncClient client) {
        this.client = client;
    }

    /** 适用于 IDEA 和 Maven 的普通 classpath；不启动整个 Spring Boot 应用。 */
    public static ConversionMcpClient startLocal() {
        String executable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        var parameters = ServerParameters.builder(executable)
                .args("-Dlogback.configurationFile=logback-mcp.xml", "-cp", classpath,
                        ConversionMcpServerMain.class.getName());
        for (String name : List.of("AGENT_MYSQL_USER", "AGENT_MYSQL_PASSWORD")) {
            if (System.getenv(name) != null) {
                parameters.addEnvVar(name, System.getenv(name));
            }
        }
        return new ConversionMcpClient(parameters.build(), Duration.ofSeconds(20));
    }

    public List<McpSchema.Tool> listTools() {
        List<McpSchema.Tool> tools = new ArrayList<>();
        Set<String> cursors = new HashSet<>();
        String cursor = null;
        do {
            McpSchema.ListToolsResult page = client.listTools(cursor);
            if (page == null || page.tools() == null) {
                throw new IllegalStateException("MCP tools/list 缺少工具列表");
            }
            tools.addAll(page.tools());
            cursor = page.nextCursor();
            if (cursor != null && (!cursors.add(cursor) || cursors.size() > 32)) {
                throw new IllegalStateException("MCP tools/list 分页游标重复或超过上限");
            }
        } while (cursor != null);
        return List.copyOf(tools);
    }

    public Map<String, Object> querySql(String sql) {
        McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                ConversionMcpServer.TOOL_NAME, Map.of("sql", sql)));
        if (result == null) throw new IllegalStateException("MCP 未返回工具结果");
        if (Boolean.TRUE.equals(result.isError())) {
            if ("connection".equals(mapper.valueToTree(result.structuredContent()).path("errorType").asText())) {
                throw new IllegalStateException("MCP 数据库连接中断");
            }
            String message = result.content() == null ? "SQL 查询失败" : result.content().stream()
                    .filter(McpSchema.TextContent.class::isInstance).map(McpSchema.TextContent.class::cast)
                    .map(McpSchema.TextContent::text).collect(java.util.stream.Collectors.joining("\n"));
            throw new SqlToolException(message.isBlank() ? "SQL 查询失败" : message);
        }
        JsonNode data = mapper.valueToTree(result.structuredContent());
        if (data == null || !data.isObject() || !data.path("columns").isArray() || !data.path("rows").isArray()
                || !data.path("rowCount").isIntegralNumber() || !data.path("rowCount").canConvertToInt()
                || data.path("rowCount").intValue() != data.path("rows").size()
                || data.path("rows").size() > SqlQueryTool.MAX_ROWS || !data.path("truncated").isBoolean()) {
            throw new IllegalStateException("MCP 查询结果契约不兼容");
        }
        for (JsonNode column : data.path("columns")) {
            if (!column.isObject() || !column.path("name").isTextual()
                    || !column.path("jdbcType").isIntegralNumber() || !column.path("typeName").isTextual()) {
                throw new IllegalStateException("MCP 列元数据无效");
            }
        }
        for (JsonNode row : data.path("rows")) {
            if (!row.isArray() || row.size() != data.path("columns").size()) {
                throw new IllegalStateException("MCP 行与列数量不一致");
            }
        }
        return mapper.convertValue(data, new TypeReference<Map<String, Object>>() { });
    }

    /** 可反馈给模型修正的工具错误，与传输或结果契约错误区分。 */
    public static class SqlToolException extends RuntimeException {
        public SqlToolException(String message) {
            super(message);
        }
    }

    @Override
    public void close() {
        if (!client.closeGracefully()) {
            throw new IllegalStateException("MCP Client 未正常关闭，请检查日志");
        }
    }
}
