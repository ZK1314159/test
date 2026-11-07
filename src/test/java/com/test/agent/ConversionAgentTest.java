package com.test.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.test.service.direct.AiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.test.agent.mcp.ConversionMcpClient;
import com.test.agent.mcp.ConversionMcpServer;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConversionAgentTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AiClient client = mock(AiClient.class);
    private final ConversionMcpClient tool = mock(ConversionMcpClient.class);
    private final LocalDateTime end = LocalDateTime.of(2026, 11, 1, 16, 0);

    @BeforeEach
    void discoverTool() {
        when(tool.listTools()).thenReturn(List.of(ConversionMcpServer.toolDefinition()));
    }

    @Test
    void modelChoosesTwoWindowsAndReceivesBothResultsWithMatchingCallIds() throws Exception {
        JsonNode first = call("call_current", "query_conversion_summary", "2026-11-01T15:00:00", "2026-11-01T16:00:00");
        ((ArrayNode) first.path("output")).insert(0, mapper.readTree(
                "{\"type\":\"reasoning\",\"id\":\"rs_test\",\"summary\":[],\"encrypted_content\":\"opaque-test-state\"}"));
        when(client.respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any()))
                .thenReturn(first,
                        call("call_previous", "query_conversion_summary", "2026-11-01T14:00:00", "2026-11-01T15:00:00"),
                        answer("成功率从98%下降到80%，下降18个百分点；仍需业务阈值。"));
        when(tool.querySummary(end.minusHours(1), end)).thenReturn(Collections.singletonMap("successRate", 0.8));
        when(tool.querySummary(end.minusHours(2), end.minusHours(1))).thenReturn(Collections.singletonMap("successRate", 0.98));

        McpSchema.Tool discovered = McpSchema.Tool.builder()
                .name("query_conversion_summary").description("来自 MCP tools/list 的描述")
                .inputSchema(ConversionMcpServer.toolDefinition().inputSchema()).build();
        when(tool.listTools()).thenReturn(List.of(discovered));
        AgentTrace trace = new AgentTrace(mapper);
        Map<String, Object> result = new ConversionAgent(tool, client, mapper).run("分析回传", end, trace);

        assertEquals(2, ((java.util.List<?>) result.get("observations")).size());
        ArgumentCaptor<ArrayNode> inputs = ArgumentCaptor.forClass(ArrayNode.class);
        verify(client, times(3)).respond(anyString(), inputs.capture(), any(ArrayNode.class), any());
        ArgumentCaptor<ArrayNode> definitions = ArgumentCaptor.forClass(ArrayNode.class);
        verify(client, times(3)).respond(anyString(), any(ArrayNode.class), definitions.capture(), any());
        assertEquals("来自 MCP tools/list 的描述", definitions.getValue().get(0).path("description").asText());
        assertEquals(mapper.valueToTree(discovered.inputSchema()),
                definitions.getValue().get(0).path("parameters"));
        verify(tool, times(1)).listTools();
        assertEquals("mcp_tools_list", trace.document().path("events").get(0).path("kind").asText());
        ArrayNode last = inputs.getAllValues().get(2);
        assertEquals("opaque-test-state", last.get(1).path("encrypted_content").asText());
        assertEquals("call_current", last.get(3).path("call_id").asText());
        assertEquals(0.8, mapper.readTree(last.get(3).path("output").asText()).path("successRate").asDouble());
        assertEquals("call_previous", last.get(5).path("call_id").asText());
        verify(tool).querySummary(end.minusHours(1), end);
        verify(tool).querySummary(end.minusHours(2), end.minusHours(1));
    }

    @Test
    void unknownToolNeverExecutesSql() throws Exception {
        when(client.respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any()))
                .thenReturn(call("call_bad", "execute_sql", "2026-11-01T15:00:00", "2026-11-01T16:00:00"));
        assertThrows(IllegalArgumentException.class,
                () -> new ConversionAgent(tool, client, mapper).run("分析", end, new AgentTrace(mapper)));
        verify(tool, never()).querySummary(any(), any());
    }

    @Test
    void invalidArgumentsNeverExecuteSql() throws Exception {
        String[] arguments = {
                "{\"start\":\"2026-10-31T15:00:00\",\"end\":\"2026-10-31T16:00:00\"}",
                "{\"start\":\"2026-11-01T14:00:00\",\"end\":\"2026-11-01T16:00:00\"}",
                "{\"start\":\"2026-11-01T16:00:00\",\"end\":\"2026-11-01T15:00:00\"}",
                "{\"start\":\"2026-11-01T15:00:00\",\"end\":\"2026-11-01T17:00:00\"}",
                "{\"start\":\"bad-date\",\"end\":\"2026-11-01T16:00:00\"}",
                "{\"start\":15,\"end\":\"2026-11-01T16:00:00\"}",
                "{\"start\":\"2026-11-01T15:00:00\"}",
                "{\"start\":\"2026-11-01T15:00:00\",\"end\":\"2026-11-01T16:00:00\",\"sql\":\"DELETE\"}",
                "{} trailing-data"
        };
        for (String argumentsJson : arguments) {
            JsonNode response = call("call_bad", "query_conversion_summary", "2026-11-01T15:00:00", "2026-11-01T16:00:00");
            ((com.fasterxml.jackson.databind.node.ObjectNode) response.path("output").get(0)).put("arguments", argumentsJson);
            when(client.respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any())).thenReturn(response);
            assertThrows(Exception.class,
                    () -> new ConversionAgent(tool, client, mapper).run("分析", end, new AgentTrace(mapper)), argumentsJson);
        }
        verify(tool, never()).querySummary(any(), any());
    }

    @Test
    void refusesFinalAnalysisWithoutAnyObservation() throws Exception {
        when(client.respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any()))
                .thenReturn(answer("一切正常"));
        assertThrows(IllegalStateException.class,
                () -> new ConversionAgent(tool, client, mapper).run("分析", end, new AgentTrace(mapper)));
        verify(tool, never()).querySummary(any(), any());
    }

    @Test
    void repeatedCallIdAndQueryFailureAreNotSilentlyConvertedToSuccess() throws Exception {
        JsonNode response = call("call_same", "query_conversion_summary", "2026-11-01T15:00:00", "2026-11-01T16:00:00");
        when(client.respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any())).thenReturn(response);
        when(tool.querySummary(any(), any())).thenReturn(Collections.singletonMap("total", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConversionAgent(tool, client, mapper).run("分析", end, new AgentTrace(mapper)));
        verify(tool, times(1)).querySummary(any(), any());

        RuntimeException dbFailure = new IllegalStateException("database unavailable");
        when(tool.querySummary(any(), any())).thenThrow(dbFailure);
        assertSame(dbFailure, assertThrows(IllegalStateException.class,
                () -> new ConversionAgent(tool, client, mapper).run("分析", end, new AgentTrace(mapper))));
    }

    @Test
    void stopsAtToolCallBudget() throws Exception {
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        when(client.respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any()))
                .thenAnswer(invocation -> call("call_" + counter.incrementAndGet(), "query_conversion_summary",
                        "2026-11-01T15:00:00", "2026-11-01T16:00:00"));
        when(tool.querySummary(any(), any())).thenReturn(Collections.singletonMap("total", 0));
        assertThrows(IllegalStateException.class,
                () -> new ConversionAgent(tool, client, mapper).run("分析", end, new AgentTrace(mapper)));
        verify(client, times(4)).respond(anyString(), any(ArrayNode.class), any(ArrayNode.class), any());
        verify(tool, times(3)).querySummary(any(), any());
    }

    @Test
    void traceTakesSnapshotsInsteadOfKeepingMutableReferences() {
        AgentTrace trace = new AgentTrace(mapper);
        ArrayNode input = mapper.createArrayNode().add("first");
        trace.record(Collections.singletonMap("input", input));
        input.add("later");
        assertEquals(1, trace.document().path("events").get(0).path("input").size());
    }

    @Test
    void missingOrIncompatibleDiscoveredToolStopsBeforeModelRequest() {
        when(tool.listTools()).thenReturn(List.of());
        assertThrows(IllegalStateException.class,
                () -> new ConversionAgent(tool, client, mapper).run("分析", end, new AgentTrace(mapper)));
        var wrong = McpSchema.Tool.builder().name("query_conversion_summary")
                .inputSchema(new McpSchema.JsonSchema("object", Map.of("sql", Map.of("type", "string")),
                        List.of("sql"), false, null, null)).build();
        when(tool.listTools()).thenReturn(List.of(wrong));
        assertThrows(IllegalStateException.class,
                () -> new ConversionAgent(tool, client, mapper).run("分析", end, new AgentTrace(mapper)));
        verifyNoInteractions(client);
        verify(tool, never()).querySummary(any(), any());
    }

    private JsonNode call(String id, String name, String start, String finish) throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode response = mapper.createObjectNode();
        response.put("status", "completed");
        Map<String, String> arguments = new java.util.LinkedHashMap<>();
        arguments.put("start", start);
        arguments.put("end", finish);
        response.putArray("output").addObject().put("type", "function_call").put("call_id", id)
                .put("name", name).put("arguments", mapper.writeValueAsString(arguments));
        return response;
    }

    private JsonNode answer(String text) {
        com.fasterxml.jackson.databind.node.ObjectNode response = mapper.createObjectNode();
        response.put("status", "completed");
        response.putArray("output").addObject().put("type", "message").put("role", "assistant")
                .putArray("content").addObject().put("type", "output_text").put("text", text);
        return response;
    }
}
