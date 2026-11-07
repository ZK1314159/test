package com.test.agent.mcp;

import com.test.agent.ConversionUploadLogTool;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversionMcpServerTest {
    private final LocalDateTime anchor = LocalDateTime.parse("2026-11-01T16:00:00");
    private final ConversionUploadLogTool query = mock(ConversionUploadLogTool.class);

    @Test
    void validCallDelegatesAndReturnsStructuredAndTextEvidence() {
        when(query.querySummary(anchor.minusHours(1), anchor)).thenReturn(Map.of("total", 100));
        var result = new ConversionMcpServer(query, anchor).specification().callHandler().apply(null,
                new McpSchema.CallToolRequest("query_conversion_summary", arguments("15:00:00", "16:00:00")));
        assertFalse(result.isError());
        assertEquals(Map.of("total", 100), result.structuredContent());
        assertTrue(((McpSchema.TextContent) result.content().get(0)).text().contains("100"));
        verify(query).querySummary(anchor.minusHours(1), anchor);
    }

    @Test
    void invalidArgumentsAndUnauthorizedWindowsNeverQueryDatabase() {
        List<Map<String, Object>> invalid = List.of(
                Map.of(), Map.of("start", "bad-date", "end", anchor.toString()),
                Map.of("start", 15, "end", anchor.toString()),
                Map.of("start", "2026-11-01T15:00:00", "end", anchor.toString(), "sql", "DELETE"),
                arguments("13:00:00", "14:00:00"), arguments("14:00:00", "16:00:00"),
                arguments("15:00:00", "17:00:00"), arguments("16:00:00", "15:00:00"),
                arguments("16:00:00", "16:00:00"));
        var handler = new ConversionMcpServer(query, anchor).specification().callHandler();
        for (var args : invalid) {
            var result = handler.apply(null, new McpSchema.CallToolRequest("query_conversion_summary", args));
            assertTrue(result.isError(), args.toString());
            assertNull(result.structuredContent());
        }
        verifyNoInteractions(query);
    }

    @Test
    void databaseFailureIsAnErrorRatherThanAnEmptySummary() {
        when(query.querySummary(any(), any())).thenThrow(new IllegalStateException("database unavailable"));
        var result = new ConversionMcpServer(query, anchor).specification().callHandler().apply(null,
                new McpSchema.CallToolRequest("query_conversion_summary", arguments("15:00:00", "16:00:00")));
        assertTrue(result.isError());
        assertNull(result.structuredContent());
        assertFalse(result.content().toString().contains("database unavailable"));
    }

    private Map<String, Object> arguments(String start, String end) {
        return Map.of("start", "2026-11-01T" + start, "end", "2026-11-01T" + end);
    }
}
