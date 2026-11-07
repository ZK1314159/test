package com.test.agent.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConversionMcpClientTest {
    private final McpSyncClient protocol = mock(McpSyncClient.class);
    private final ConversionMcpClient client = new ConversionMcpClient(protocol);
    private final LocalDateTime anchor = LocalDateTime.parse("2026-11-01T16:00:00");

    @Test
    void discoversEveryPageAndRejectsRepeatedCursor() {
        when(protocol.listTools(isNull())).thenReturn(new McpSchema.ListToolsResult(List.of(), "page2"));
        when(protocol.listTools("page2")).thenReturn(new McpSchema.ListToolsResult(
                List.of(ConversionMcpServer.toolDefinition()), null));
        assertEquals(1, client.listTools().size());
        when(protocol.listTools("page2")).thenReturn(new McpSchema.ListToolsResult(List.of(), "page2"));
        assertThrows(IllegalStateException.class, client::listTools);
    }

    @Test
    void errorsAndMalformedOrInconsistentEvidenceAreRejected() {
        List<McpSchema.CallToolResult> invalid = List.of(
                McpSchema.CallToolResult.builder().isError(true).addTextContent("failed").build(),
                McpSchema.CallToolResult.builder().addTextContent("not structured").build(),
                McpSchema.CallToolResult.builder().structuredContent(Map.of("total", 100)).build(),
                result(100, 80, 0.1), result(0, 0, 0.0), result(10, 80, 0.8));
        for (var response : invalid) {
            when(protocol.callTool(any())).thenReturn(response);
            assertThrows(IllegalStateException.class, () -> client.querySummary(anchor.minusHours(1), anchor));
        }
    }

    @Test
    void protocolFailurePreservesOriginalException() {
        RuntimeException failure = new IllegalStateException("transport closed");
        when(protocol.callTool(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> client.querySummary(anchor.minusHours(1), anchor)));
    }

    private McpSchema.CallToolResult result(long total, long success, Object rate) {
        return McpSchema.CallToolResult.builder().structuredContent(Map.of(
                "start", anchor.minusHours(1).toString(), "end", anchor.toString(), "timeBasis", "created_at",
                "total", total, "success", success, "failed", 20, "initialized", 0,
                "processing", 0, "unknown", 0, "successRate", rate)).build();
    }
}
