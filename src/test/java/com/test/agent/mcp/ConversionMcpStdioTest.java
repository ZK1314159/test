package com.test.agent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.test.agent.AgentTrace;
import com.test.agent.ConversionAgent;
import com.test.service.direct.AiClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(25)
class ConversionMcpStdioTest {
    private final LocalDateTime anchor = LocalDateTime.parse("2026-11-01T16:00:00");

    @Test
    void discoversToolsAndCallsRealSubprocessIncludingEmptyWindow() {
        try (var mcp = connect("normal", Duration.ofSeconds(10))) {
            var tools = mcp.listTools();
            assertEquals(1, tools.size());
            assertEquals("query_conversion_summary", tools.get(0).name());
            assertEquals("string", ((java.util.Map<?, ?>) tools.get(0).inputSchema().properties().get("start")).get("type"));
            var summary = mcp.querySummary(anchor.minusHours(1), anchor);
            assertEquals(100, ((Number) summary.get("total")).intValue());
            assertEquals(0.8, summary.get("successRate"));
            var empty = mcp.querySummary(anchor.minusHours(2), anchor.minusHours(1));
            assertEquals(0, ((Number) empty.get("total")).intValue());
            assertNull(empty.get("successRate"));
            assertThrows(IllegalStateException.class, () -> mcp.querySummary(anchor.minusDays(1), anchor));
        }
    }

    @Test
    void queryFailureIsNotAnEmptySummary() {
        try (var mcp = connect("failure", Duration.ofSeconds(10))) {
            assertThrows(IllegalStateException.class, () -> mcp.querySummary(anchor.minusHours(1), anchor));
        }
    }

    @Test
    void unknownToolAndExtraArgumentsAreRejectedOverTheWire() {
        var transport = new StdioClientTransport(ServerParameters.builder(executable())
                .args("-cp", classpath(), ConversionMcpTestServer.class.getName(), "normal").build(),
                new JacksonMcpJsonMapper(new ObjectMapper()));
        var rawClient = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
        try {
            rawClient.initialize();
            assertThrows(RuntimeException.class, () -> rawClient.callTool(
                    new McpSchema.CallToolRequest("execute_sql", java.util.Map.of("sql", "DELETE"))));
            var invalid = rawClient.callTool(new McpSchema.CallToolRequest("query_conversion_summary",
                    java.util.Map.of("start", anchor.minusHours(1).toString(), "end", anchor.toString(),
                            "sql", "DELETE")));
            assertTrue(invalid.isError());
            assertNull(invalid.structuredContent());
        } finally {
            assertTrue(rawClient.closeGracefully());
        }
    }

    @Test
    void slowQueryStopsAtClientTimeout() {
        try (var mcp = connect("slow", Duration.ofSeconds(2))) {
            assertThrows(RuntimeException.class, () -> mcp.querySummary(anchor.minusHours(1), anchor));
        }
    }

    @Test
    void agentDiscoversAndUsesMcpEvidenceWithOriginalModelCallId() throws Exception {
        var mapper = new ObjectMapper();
        var model = mock(AiClient.class);
        var call = mapper.createObjectNode().put("status", "completed");
        call.putArray("output").addObject().put("type", "function_call").put("call_id", "model_call_1")
                .put("name", "query_conversion_summary")
                .put("arguments", "{\"start\":\"2026-11-01T15:00:00\",\"end\":\"2026-11-01T16:00:00\"}");
        var answer = mapper.createObjectNode().put("status", "completed");
        answer.putArray("output").addObject().put("type", "message").put("role", "assistant")
                .putArray("content").addObject().put("type", "output_text").put("text", "成功率80%，缺少判断阈值。");
        when(model.respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any())).thenReturn(call, answer);
        try (var mcp = connect("normal", Duration.ofSeconds(10))) {
            var trace = new AgentTrace(mapper);
            var result = new ConversionAgent(mcp, model, mapper).run("分析回传", anchor, trace);
            assertEquals(1, result.get("toolCalls"));
            ArgumentCaptor<ArrayNode> inputs = ArgumentCaptor.forClass(ArrayNode.class);
            verify(model, times(2)).respond(anyString(), inputs.capture(), any(ArrayNode.class), any());
            var toolOutput = inputs.getValue().get(2);
            assertEquals("model_call_1", toolOutput.path("call_id").asText());
            assertEquals(100, mapper.readTree(toolOutput.path("output").asText()).path("total").asInt());
            assertTrue(trace.document().path("events").findValuesAsText("kind").contains("mcp_tools_list"));
        }
    }

    @Test
    void eofEndsServerAndStartupLogsDoNotPolluteStdout() throws Exception {
        Process process = new ProcessBuilder(executable(), "-cp", classpath(),
                ConversionMcpTestServer.class.getName(), "normal")
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            process.getOutputStream().close();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "stdin EOF 后服务端必须退出");
            assertEquals(0, process.exitValue());
            assertEquals(0, process.getInputStream().readAllBytes().length);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    private ConversionMcpClient connect(String mode, Duration timeout) {
        return new ConversionMcpClient(ServerParameters.builder(executable())
                .args("-cp", classpath(), ConversionMcpTestServer.class.getName(), mode).build(), timeout);
    }

    private String executable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private String classpath() {
        return System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    }
}
