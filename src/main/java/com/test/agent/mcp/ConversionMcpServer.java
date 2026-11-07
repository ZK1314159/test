package com.test.agent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/** stdio 协议入口：仅开放通用只读 SQL 工具。 */
public class ConversionMcpServer {
    public static final String TOOL_NAME = "query_sql";
    private static final Logger log = LoggerFactory.getLogger(ConversionMcpServer.class);
    private final SqlQueryTool query;
    private final ObjectMapper mapper = new ObjectMapper();

    public ConversionMcpServer(SqlQueryTool query) {
        this.query = Objects.requireNonNull(query);
    }

    public static McpSchema.Tool toolDefinition() {
        return McpSchema.Tool.builder().name(TOOL_NAME)
                .description("执行本机 MySQL 单条只读 SQL，默认库 tdtracking。可查询任意表、JOIN、聚合、子查询、CTE，"
                        + "或通过 SHOW TABLES、SHOW COLUMNS、DESCRIBE、information_schema 探索结构。"
                        + "不允许写入、锁定或文件输出。每次超时10秒，最多200行、1 MiB；truncated表示结果被截断。")
                .inputSchema(new McpSchema.JsonSchema("object", Map.of(
                        "sql", Map.of("type", "string", "description", "完整的单条 MySQL 只读 SQL")),
                        List.of("sql"), false, null, null)).build();
    }

    public McpServerFeatures.SyncToolSpecification specification() {
        return new McpServerFeatures.SyncToolSpecification(toolDefinition(), (exchange, request) -> handle(request));
    }

    // 服务复用单连接，串行查询，不在等待模型时持有事务。
    private synchronized McpSchema.CallToolResult handle(McpSchema.CallToolRequest request) {
        try {
            Map<String, Object> args = request.arguments();
            if (!TOOL_NAME.equals(request.name()) || args == null || !args.keySet().equals(Set.of("sql"))
                    || !(args.get("sql") instanceof String sql) || sql.isBlank()) {
                throw new IllegalArgumentException("仅允许一个非空 sql 字符串参数");
            }
            Map<String, Object> result = query.querySql((String) args.get("sql"));
            return McpSchema.CallToolResult.builder().structuredContent(result)
                    .addTextContent(mapper.writeValueAsString(result)).isError(false).build();
        } catch (IllegalArgumentException e) {
            log.warn("MCP SQL 查询被拒绝，tool={}", TOOL_NAME, e);
            return error(e.getMessage());
        } catch (Exception e) {
            log.error("MCP SQL 查询失败，tool={}", TOOL_NAME, e);
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLException sql && sql.getSQLState() != null
                        && sql.getSQLState().startsWith("08")) {
                    return McpSchema.CallToolResult.builder().isError(true)
                            .structuredContent(Map.of("errorType", "connection"))
                            .addTextContent("数据库连接中断，请检查服务端连接").build();
                }
            }
            return error(databaseError(e));
        }
    }

    private McpSchema.CallToolResult error(String message) {
        return McpSchema.CallToolResult.builder().isError(true).addTextContent(message).build();
    }

    private String databaseError(Exception failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql) {
                String hint = switch (sql.getErrorCode()) {
                    case 1064 -> "SQL 语法错误，请调整 MySQL 语法";
                    case 1054 -> "字段不存在，请先查询表结构";
                    case 1146 -> "表不存在，请先查询库表结构";
                    case 1052 -> "字段名不明确，请补充表别名";
                    case 1142, 1227 -> "当前数据库账户没有查询权限";
                    case 3024, 1317 -> "查询超时或被取消，请缩小查询范围";
                    default -> sql instanceof java.sql.SQLTimeoutException ? "查询超时，请缩小查询范围"
                            : "数据库查询失败，请检查语法、表结构或连接状态";
                };
                // 不返回驱动原始消息，其中可能含连接信息或数据。
                String state = sql.getSQLState();
                return hint + "；SQLState=" + (state != null && state.matches("[A-Z0-9]{5}") ? state : "unknown")
                        + "，errorCode=" + sql.getErrorCode();
            }
        }
        return "查询失败，请查看服务端日志";
    }

    public void serve(InputStream input, OutputStream output) throws InterruptedException {
        CountDownLatch disconnected = new CountDownLatch(1);
        // stdin 关闭后结束服务，避免只剩等待线程的孤儿进程。
        InputStream monitoredInput = new FilterInputStream(input) {
            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                try {
                    int count = super.read(bytes, offset, length);
                    if (count == -1) {
                        disconnected.countDown();
                    }
                    return count;
                } catch (IOException e) {
                    disconnected.countDown();
                    log.error("MCP stdin 读取失败", e);
                    throw e;
                }
            }
        };
        var jsonMapper = new JacksonMcpJsonMapper(mapper);
        var transport = new StdioServerTransportProvider(jsonMapper, monitoredInput, output);
        var server = McpServer.sync(transport).jsonMapper(jsonMapper)
                .serverInfo("sql-query-server", "2.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                .tools(specification()).build();
        try {
            disconnected.await();
        } finally {
            server.close();
        }
    }
}
