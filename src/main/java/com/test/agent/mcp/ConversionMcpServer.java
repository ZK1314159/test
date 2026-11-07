package com.test.agent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.agent.ConversionUploadLogTool;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/** 协议入口：声明工具、校验授权窗口，再调用原来的 JDBC 查询。 */
public class ConversionMcpServer {
    public static final String TOOL_NAME = "query_conversion_summary";
    private static final Logger log = LoggerFactory.getLogger(ConversionMcpServer.class);
    private final ConversionUploadLogTool query;
    private final LocalDateTime anchor;
    private final ObjectMapper mapper = new ObjectMapper();

    public ConversionMcpServer(ConversionUploadLogTool query, LocalDateTime anchor) {
        this.query = Objects.requireNonNull(query);
        this.anchor = Objects.requireNonNull(anchor);
    }

    public static McpSchema.Tool toolDefinition() {
        return McpSchema.Tool.builder()
                .name(TOOL_NAME)
                .description("只读查询指定时间窗口创建的回传记录的当前状态统计；每次至多一小时，"
                        + "仅限服务端锚点前两小时。按数据库当地时间解释，created_at 左闭右开，"
                        + "不是发送尝试次数；successRate 为 0 到 1，null 表示无数据。")
                .inputSchema(new McpSchema.JsonSchema("object", Map.of(
                        "start", Map.of("type", "string", "description", "开始时间，包含，ISO本地时间"),
                        "end", Map.of("type", "string", "description", "结束时间，不包含，ISO本地时间")),
                        List.of("start", "end"), false, null, null))
                .build();
    }

    public McpServerFeatures.SyncToolSpecification specification() {
        return new McpServerFeatures.SyncToolSpecification(toolDefinition(), (exchange, request) -> handle(request));
    }

    // 本教学服务复用单连接，串行执行查询，不在等待模型时持有事务。
    private synchronized McpSchema.CallToolResult handle(McpSchema.CallToolRequest request) {
        try {
            Map<String, Object> args = request.arguments();
            if (!TOOL_NAME.equals(request.name()) || args == null
                    || !args.keySet().equals(Set.of("start", "end"))
                    || !(args.get("start") instanceof String) || !(args.get("end") instanceof String)) {
                throw new IllegalArgumentException("仅允许 start/end 两个时间字符串");
            }
            LocalDateTime start = LocalDateTime.parse((String) args.get("start"));
            LocalDateTime end = LocalDateTime.parse((String) args.get("end"));
            if (!start.isBefore(end) || start.isBefore(anchor.minusHours(2)) || end.isAfter(anchor)
                    || Duration.between(start, end).compareTo(Duration.ofHours(1)) > 0) {
                throw new IllegalArgumentException("时间必须位于服务端锚点前两小时，且每次大于零、不超过一小时");
            }
            Map<String, Object> summary = query.querySummary(start, end);
            return McpSchema.CallToolResult.builder().structuredContent(summary)
                    .addTextContent(mapper.writeValueAsString(summary)).isError(false).build();
        } catch (IllegalArgumentException e) {
            log.warn("MCP 查询参数被拒绝，tool={}，anchor={}", TOOL_NAME, anchor, e);
            return McpSchema.CallToolResult.builder().isError(true)
                    .addTextContent("时间参数无效：仅允许 start/end，范围为锚点前两小时，每次至多一小时").build();
        } catch (Exception e) {
            log.error("MCP 统计查询失败，tool={}，anchor={}", TOOL_NAME, anchor, e);
            return McpSchema.CallToolResult.builder().isError(true)
                    .addTextContent("统计查询失败，请查看服务端日志；不能将失败当作零条记录").build();
        }
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
                .serverInfo("conversion-summary-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                .tools(specification()).build();
        try {
            disconnected.await();
        } finally {
            server.close();
        }
    }
}
