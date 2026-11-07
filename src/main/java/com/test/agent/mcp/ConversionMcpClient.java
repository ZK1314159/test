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
import java.time.LocalDateTime;
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
    public static ConversionMcpClient startLocal(LocalDateTime anchor) {
        String executable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        var parameters = ServerParameters.builder(executable)
                .args("-Dlogback.configurationFile=logback-mcp.xml", "-cp", classpath,
                        ConversionMcpServerMain.class.getName())
                .addEnvVar("AGENT_WINDOW_END", anchor.toString());
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

    public Map<String, Object> querySummary(LocalDateTime start, LocalDateTime end) {
        McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                ConversionMcpServer.TOOL_NAME, Map.of("start", start.toString(), "end", end.toString())));
        if (result == null || Boolean.TRUE.equals(result.isError()) || result.structuredContent() == null) {
            throw new IllegalStateException("MCP 统计查询失败或未返回结构化证据，不能作为空数据处理");
        }
        JsonNode summary = mapper.valueToTree(result.structuredContent());
        validateSummary(summary, start, end);
        return mapper.convertValue(summary, new TypeReference<Map<String, Object>>() { });
    }

    private void validateSummary(JsonNode summary, LocalDateTime start, LocalDateTime end) {
        if (!summary.isObject() || !start.toString().equals(summary.path("start").asText())
                || !end.toString().equals(summary.path("end").asText())
                || !"created_at".equals(summary.path("timeBasis").asText())) {
            throw new IllegalStateException("MCP 统计结果的时间窗口或统计口径不匹配");
        }
        long accounted = 0;
        for (String field : List.of("total", "success", "failed", "initialized", "processing", "unknown")) {
            JsonNode value = summary.path(field);
            if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
                throw new IllegalStateException("MCP 统计结果缺少有效计数：" + field);
            }
            if (!"total".equals(field)) {
                accounted = Math.addExact(accounted, value.longValue());
            }
        }
        long total = summary.path("total").longValue();
        JsonNode rate = summary.path("successRate");
        if (accounted != total || (total == 0 ? !rate.isNull()
                : !rate.isNumber() || !Double.isFinite(rate.doubleValue())
                || Math.abs(rate.doubleValue() - summary.path("success").doubleValue() / total) > 1e-9)) {
            throw new IllegalStateException("MCP 统计结果的计数或成功率不一致");
        }
    }

    @Override
    public void close() {
        if (!client.closeGracefully()) {
            throw new IllegalStateException("MCP Client 未正常关闭，请检查日志");
        }
    }
}
