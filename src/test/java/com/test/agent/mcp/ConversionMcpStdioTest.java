package com.test.agent.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.test.agent.AgentTrace;
import com.test.agent.ConversionAgent;
import com.test.service.direct.AiClient;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
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

@Timeout(25)
class ConversionMcpStdioTest {
    private final LocalDateTime anchor = LocalDateTime.parse("2026-11-01T16:00:00");

    @Test
    void discoversToolsAndCallsRealSubprocessIncludingEmptyWindow() {
        try (var mcp = connect("normal", Duration.ofSeconds(10))) {
            var tools = mcp.listTools();
            assertEquals(1, tools.size());
            assertEquals("query_sql", tools.get(0).name());
            assertEquals(
                    "string",
                    ((java.util.Map<?, ?>) tools.get(0).inputSchema().properties().get("sql"))
                            .get("type"));
            var data = mcp.querySql("SELECT 1");
            assertEquals(1, data.get("rowCount"));
            var empty = mcp.querySql("SELECT 1 WHERE 1=0");
            assertEquals(0, empty.get("rowCount"));
            assertEquals(false, empty.get("truncated"));
            assertThrows(
                    ConversionMcpClient.SqlToolException.class,
                    () -> mcp.querySql("DELETE FROM foo"));
        }
    }

    @Test
    void queryFailureIsNotAnEmptySummary() {
        try (var mcp = connect("failure", Duration.ofSeconds(10))) {
            assertThrows(
                    ConversionMcpClient.SqlToolException.class, () -> mcp.querySql("SELECT 1"));
        }
    }

    @Test
    void unknownToolAndExtraArgumentsAreRejectedOverTheWire() {
        var transport =
                new StdioClientTransport(
                        ServerParameters.builder(executable())
                                .args(
                                        "-cp",
                                        classpath(),
                                        ConversionMcpTestServer.class.getName(),
                                        "normal")
                                .build(),
                        new JacksonMcpJsonMapper(new ObjectMapper()));
        var rawClient = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
        try {
            rawClient.initialize();
            assertThrows(
                    RuntimeException.class,
                    () ->
                            rawClient.callTool(
                                    new McpSchema.CallToolRequest(
                                            "query_conversion_summary",
                                            java.util.Map.of("start", "x", "end", "y"))));
            var invalid =
                    rawClient.callTool(
                            new McpSchema.CallToolRequest(
                                    "query_sql", java.util.Map.of("sql", "SELECT 1", "extra", 1)));
            assertTrue(invalid.isError());
            assertNull(invalid.structuredContent());
        } finally {
            assertTrue(rawClient.closeGracefully());
        }
    }

    @Test
    void slowQueryStopsAtClientTimeout() {
        try (var mcp = connect("slow", Duration.ofSeconds(2))) {
            assertThrows(RuntimeException.class, () -> mcp.querySql("SELECT 1"));
        }
    }

    @Test
    void agentDiscoversAndUsesMcpEvidenceWithOriginalModelCallId() throws Exception {
        var mapper = new ObjectMapper();
        var model = mock(AiClient.class);
        var call = mapper.createObjectNode().put("status", "completed");
        call.putArray("output")
                .addObject()
                .put("type", "function_call")
                .put("call_id", "model_call_1")
                .put("name", "query_sql")
                .put("arguments", "{\"sql\":\"SELECT 1\"}");
        var answer = mapper.createObjectNode().put("status", "completed");
        answer.putArray("output")
                .addObject()
                .put("type", "message")
                .put("role", "assistant")
                .putArray("content")
                .addObject()
                .put("type", "output_text")
                .put("text", "查询返回一行，值为1。");
        when(model.respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any()))
                .thenReturn(call, answer);
        try (var mcp = connect("normal", Duration.ofSeconds(10))) {
            var trace = new AgentTrace(mapper);
            var result = new ConversionAgent(mcp, model, mapper).run("分析回传", anchor, trace);
            assertEquals(1, result.get("toolCalls"));
            ArgumentCaptor<ArrayNode> inputs = ArgumentCaptor.forClass(ArrayNode.class);
            verify(model, times(2))
                    .respond(anyString(), inputs.capture(), any(ArrayNode.class), any());
            var toolOutput = inputs.getValue().get(2);
            assertEquals("model_call_1", toolOutput.path("call_id").asText());
            assertEquals(
                    1,
                    mapper.readTree(toolOutput.path("output").asText()).path("rowCount").asInt());
            assertTrue(
                    trace.document()
                            .path("events")
                            .findValuesAsText("kind")
                            .contains("mcp_tools_list"));
        }
    }

    @Test
    void agentRepairsSqlAfterRealStdioToolError() throws Exception {
        var mapper = new ObjectMapper();
        var model = mock(AiClient.class);
        var calls = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        String[] statements = {"SHOW TABLES", "SELECT missing FROM foo", "SELECT 1"};
        for (int i = 0; i < statements.length; i++) {
            var response = mapper.createObjectNode();
            response.putArray("output")
                    .addObject()
                    .put("type", "function_call")
                    .put("call_id", "call_" + i)
                    .put("name", "query_sql")
                    .put(
                            "arguments",
                            mapper.writeValueAsString(java.util.Map.of("sql", statements[i])));
            calls.add(response);
        }
        var answer = mapper.createObjectNode();
        answer.putArray("output")
                .addObject()
                .put("type", "message")
                .put("role", "assistant")
                .putArray("content")
                .addObject()
                .put("type", "output_text")
                .put("text", "查询已修正，结果为1。");
        when(model.respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any()))
                .thenReturn(calls.get(0), calls.get(1), calls.get(2), answer);
        try (var mcp = connect("normal", Duration.ofSeconds(10))) {
            var result =
                    new ConversionAgent(mcp, model, mapper)
                            .run("探索数据", anchor, new AgentTrace(mapper));
            assertEquals(3, result.get("toolCalls"));
            assertEquals(4, result.get("modelRequests"));
            ArgumentCaptor<ArrayNode> inputs = ArgumentCaptor.forClass(ArrayNode.class);
            verify(model, times(4))
                    .respond(anyString(), inputs.capture(), any(ArrayNode.class), any());
            var failedOutput = inputs.getAllValues().get(2).get(4);
            assertEquals("call_1", failedOutput.path("call_id").asText());
            assertTrue(
                    mapper.readTree(failedOutput.path("output").asText())
                            .path("isError")
                            .asBoolean());
            assertTrue(failedOutput.path("output").asText().contains("字段不存在"));
        }
    }

    @Test
    void eofEndsServerAndStartupLogsDoNotPolluteStdout() throws Exception {
        Process process =
                new ProcessBuilder(
                                executable(),
                                "-cp",
                                classpath(),
                                ConversionMcpTestServer.class.getName(),
                                "normal")
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
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
        return new ConversionMcpClient(
                ServerParameters.builder(executable())
                        .args("-cp", classpath(), ConversionMcpTestServer.class.getName(), mode)
                        .build(),
                timeout);
    }

    private String executable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private String classpath() {
        return System.getProperty(
                "surefire.test.class.path", System.getProperty("java.class.path"));
    }
}
