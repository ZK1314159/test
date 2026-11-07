package com.test.agent.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.modelcontextprotocol.spec.McpSchema;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.SQLException;
import java.util.*;

class ConversionMcpServerTest {
    final SqlQueryTool query = mock(SqlQueryTool.class);

    McpSchema.CallToolResult call(String name, Map<String, Object> args) {
        return new ConversionMcpServer(query)
                .specification()
                .callHandler()
                .apply(null, new McpSchema.CallToolRequest(name, args));
    }

    @Test
    void delegatesSqlAndReturnsStructuredAndTextResults() {
        var data =
                Map.<String, Object>of(
                        "columns", List.of(), "rows", List.of(), "rowCount", 0, "truncated", false);
        when(query.querySql("SHOW TABLES")).thenReturn(data);
        var result = call("query_sql", Map.of("sql", "SHOW TABLES"));
        assertFalse(result.isError());
        assertEquals(data, result.structuredContent());
        verify(query).querySql("SHOW TABLES");
    }

    @Test
    void invalidArgumentsAndOldToolNeverExecute() {
        for (var args :
                List.<Map<String, Object>>of(
                        Map.of(),
                        Map.of("sql", 1),
                        Map.of("sql", " "),
                        Map.of("sql", "SELECT 1", "extra", 1))) {
            assertTrue(call("query_sql", args).isError());
        }
        assertTrue(call("query_conversion_summary", Map.of("sql", "SELECT 1")).isError());
        verifyNoInteractions(query);
    }

    @Test
    void marksBrokenConnectionsAsFatal() {
        when(query.querySql(anyString()))
                .thenThrow(
                        new UncategorizedSQLException(
                                "fixture", "SELECT 1", new SQLException("lost", "08003")));
        var result = call("query_sql", Map.of("sql", "SELECT 1"));
        assertTrue(result.isError());
        assertEquals(Map.of("errorType", "connection"), result.structuredContent());
    }

    @Test
    void returnsSafeActionableErrorsInsteadOfDriverMessages() {
        when(query.querySql(anyString()))
                .thenThrow(
                        new UncategorizedSQLException(
                                "secret task",
                                "secret sql",
                                new SQLException(
                                        "password=secret jdbc:mysql://secret", "42S22", 1054)));
        var result = call("query_sql", Map.of("sql", "SELECT bad FROM foo"));
        assertTrue(result.isError());
        assertNull(result.structuredContent());
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(text.contains("字段不存在"));
        assertTrue(text.contains("1054"));
        assertFalse(text.contains("secret"));
    }
}
