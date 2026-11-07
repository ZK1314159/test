package com.test.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.test.agent.mcp.*;
import com.test.service.direct.AiClient;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.*;

class ConversionAgentTest {
    final ObjectMapper mapper = new ObjectMapper();
    final AiClient model = mock(AiClient.class);
    final ConversionMcpClient tool = mock(ConversionMcpClient.class);
    final LocalDateTime anchor = LocalDateTime.parse("2026-11-01T16:00:00");

    @BeforeEach
    void discover() {
        when(tool.listTools()).thenReturn(List.of(ConversionMcpServer.toolDefinition()));
        when(tool.querySql(anyString()))
                .thenReturn(
                        Map.of(
                                "columns",
                                List.of(),
                                "rows",
                                List.of(),
                                "rowCount",
                                0,
                                "truncated",
                                false));
    }

    ConversionAgent agent() {
        return new ConversionAgent(tool, model, mapper);
    }

    Map<String, Object> run() throws Exception {
        return agent().run("分析数据", anchor, new AgentTrace(mapper));
    }

    @Test
    void exploresSchemaAndGeneratesDifferentSqlWithMatchingCallIdsAndFullHistory()
            throws Exception {
        ObjectNode first = call("schema", "SHOW TABLES");
        ((ArrayNode) first.path("output"))
                .insert(
                        0,
                        mapper.readTree(
                                "{\"type\":\"reasoning\",\"encrypted_content\":\"opaque\"}"));
        when(model.respond(anyString(), any(), any(), any()))
                .thenReturn(first, call("query", "SELECT COUNT(*) FROM foo"), answer("完成"));
        var trace = new AgentTrace(mapper);
        var result = agent().run("分析数据", anchor, trace);
        assertEquals(3, result.get("modelRequests"));
        assertEquals(2, result.get("toolCalls"));
        verify(tool).querySql("SHOW TABLES");
        verify(tool).querySql("SELECT COUNT(*) FROM foo");
        ArgumentCaptor<ArrayNode> inputs = ArgumentCaptor.forClass(ArrayNode.class);
        ArgumentCaptor<String> instructions = ArgumentCaptor.forClass(String.class);
        verify(model, times(3)).respond(instructions.capture(), inputs.capture(), any(), any());
        ArrayNode last = inputs.getValue();
        assertEquals("reasoning", last.get(1).path("type").asText());
        assertEquals("schema", last.get(3).path("call_id").asText());
        assertEquals("query", last.get(5).path("call_id").asText());
        assertEquals(1, inputs.getAllValues().get(0).size());
        assertTrue(instructions.getValue().contains("默认库为 tdtracking"));
        assertFalse(instructions.getValue().contains("前两小时"));
        assertTrue(
                trace.document().path("events").findValuesAsText("kind").contains("agent_result"));
    }

    @Test
    void sqlFailureReturnsToModelAndAllowsCorrection() throws Exception {
        when(tool.querySql("SELECT bad FROM foo"))
                .thenThrow(new ConversionMcpClient.SqlToolException("字段不存在"));
        when(model.respond(anyString(), any(), any(), any()))
                .thenReturn(
                        call("bad", "SELECT bad FROM foo"),
                        call("fixed", "SELECT * FROM foo"),
                        answer("已修正"));
        var result = run();
        assertEquals(2, result.get("toolCalls"));
        var observations = (List<?>) result.get("observations");
        assertTrue(((Map<?, ?>) observations.get(0)).containsKey("error"));
        ArgumentCaptor<ArrayNode> inputs = ArgumentCaptor.forClass(ArrayNode.class);
        verify(model, times(3)).respond(anyString(), inputs.capture(), any(), any());
        assertTrue(inputs.getAllValues().get(1).toString().contains("字段不存在"));
    }

    @Test
    void invalidParametersAreFeedbackNotTermination() throws Exception {
        for (String args :
                List.of(
                        "{}",
                        "{bad",
                        "{\"sql\":1}",
                        "{\"sql\":\"SELECT 1\",\"extra\":1}",
                        "{\"sql\":\"SELECT 1\"} trailing",
                        "{\"sql\":\"SELECT 1\",\"sql\":\"SELECT 2\"}")) {
            var bad = call("bad", "SELECT 1");
            ((ObjectNode) bad.path("output").get(0)).put("arguments", args);
            when(model.respond(anyString(), any(), any(), any())).thenReturn(bad, answer("无法执行"));
            assertEquals(0, run().get("toolCalls"));
        }
        verify(tool, never()).querySql(anyString());
    }

    @Test
    void canAnswerWithoutQueryAndContinueBeyondOldBudgets() throws Exception {
        when(model.respond(anyString(), any(), any(), any())).thenReturn(answer("一般性说明"));
        assertEquals(0, run().get("toolCalls"));
        var count = new java.util.concurrent.atomic.AtomicInteger();
        when(model.respond(anyString(), any(), any(), any()))
                .thenAnswer(
                        inv -> {
                            int n = count.incrementAndGet();
                            return n <= 6 ? call("c" + n, "SELECT " + n) : answer("完成");
                        });
        var result = run();
        assertEquals(7, result.get("modelRequests"));
        assertEquals(6, result.get("toolCalls"));
    }

    @Test
    void executesAllCallsInOneRoundAndRejectsRepeatedIds() throws Exception {
        var batch = call("a", "SELECT 1");
        ((ArrayNode) batch.path("output")).add(call("b", "SELECT 2").path("output").get(0));
        when(model.respond(anyString(), any(), any(), any())).thenReturn(batch, answer("完成"));
        assertEquals(2, run().get("toolCalls"));
        when(model.respond(anyString(), any(), any(), any())).thenReturn(call("same", "SELECT 1"));
        assertThrows(IllegalArgumentException.class, this::run);
    }

    @Test
    void transportModelAndProtocolFailuresTerminate() throws Exception {
        RuntimeException failure = new IllegalStateException("connection closed");
        when(tool.querySql(anyString())).thenThrow(failure);
        when(model.respond(anyString(), any(), any(), any())).thenReturn(call("a", "SELECT 1"));
        assertSame(failure, assertThrows(IllegalStateException.class, this::run));
        when(model.respond(anyString(), any(), any(), any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, this::run));
        doReturn(mapper.createObjectNode()).when(model).respond(anyString(), any(), any(), any());
        assertThrows(IllegalStateException.class, this::run);
    }

    @Test
    void missingOrIncompatibleToolStopsBeforeModelAndInterruptCancels() {
        when(tool.listTools()).thenReturn(List.of());
        assertThrows(IllegalStateException.class, this::run);
        var wrong =
                io.modelcontextprotocol.spec.McpSchema.Tool.builder()
                        .name("query_sql")
                        .inputSchema(
                                new io.modelcontextprotocol.spec.McpSchema.JsonSchema(
                                        "object", Map.of(), List.of(), false, null, null))
                        .build();
        when(tool.listTools()).thenReturn(List.of(wrong));
        assertThrows(IllegalStateException.class, this::run);
        verifyNoInteractions(model);
        Thread.currentThread().interrupt();
        try {
            assertThrows(java.util.concurrent.CancellationException.class, this::run);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptDuringModelResponseStopsBeforeToolExecution() throws Exception {
        when(model.respond(anyString(), any(), any(), any()))
                .thenAnswer(
                        inv -> {
                            Thread.currentThread().interrupt();
                            return call("a", "SELECT 1");
                        });
        try {
            assertThrows(java.util.concurrent.CancellationException.class, this::run);
        } finally {
            Thread.interrupted();
        }
        verify(tool, never()).querySql(anyString());
    }

    @Test
    void traceTakesSnapshots() {
        var trace = new AgentTrace(mapper);
        var value = mapper.createArrayNode().add("first");
        trace.record(Map.of("input", value));
        value.add("second");
        assertEquals(1, trace.document().path("events").get(0).path("input").size());
    }

    ObjectNode call(String id, String sql) throws Exception {
        var response = mapper.createObjectNode();
        response.putArray("output")
                .addObject()
                .put("type", "function_call")
                .put("call_id", id)
                .put("name", "query_sql")
                .put("arguments", mapper.writeValueAsString(Map.of("sql", sql)));
        return response;
    }

    ObjectNode answer(String text) {
        var response = mapper.createObjectNode();
        response.putArray("output")
                .addObject()
                .put("type", "message")
                .put("role", "assistant")
                .putArray("content")
                .addObject()
                .put("type", "output_text")
                .put("text", text);
        return response;
    }
}
