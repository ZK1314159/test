package com.test.agent.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import org.junit.jupiter.api.Test;

import java.util.*;

class ConversionMcpClientTest {
    final McpSyncClient protocol = mock(McpSyncClient.class);
    final ConversionMcpClient client = new ConversionMcpClient(protocol);

    @Test
    void discoversEveryPageAndRejectsRepeatedCursor() {
        when(protocol.listTools(isNull()))
                .thenReturn(new McpSchema.ListToolsResult(List.of(), "p2"));
        when(protocol.listTools("p2"))
                .thenReturn(
                        new McpSchema.ListToolsResult(
                                List.of(ConversionMcpServer.toolDefinition()), null));
        assertEquals("query_sql", client.listTools().get(0).name());
        when(protocol.listTools("p2")).thenReturn(new McpSchema.ListToolsResult(List.of(), "p2"));
        assertThrows(IllegalStateException.class, client::listTools);
    }

    @Test
    void toolErrorsAreDistinctFromMalformedResultsAndTransportFailures() {
        when(protocol.callTool(any()))
                .thenReturn(
                        McpSchema.CallToolResult.builder()
                                .isError(true)
                                .addTextContent("字段不存在")
                                .build());
        assertEquals(
                "字段不存在",
                assertThrows(
                                ConversionMcpClient.SqlToolException.class,
                                () -> client.querySql("SELECT bad"))
                        .getMessage());
        for (var result :
                List.of(
                        McpSchema.CallToolResult.builder().addTextContent("not structured").build(),
                        McpSchema.CallToolResult.builder()
                                .structuredContent(
                                        Map.of(
                                                "columns",
                                                List.of(),
                                                "rows",
                                                List.of(),
                                                "rowCount",
                                                1,
                                                "truncated",
                                                false))
                                .build(),
                        McpSchema.CallToolResult.builder()
                                .structuredContent(
                                        Map.of(
                                                "columns",
                                                List.of(),
                                                "rows",
                                                List.of(List.of(1)),
                                                "rowCount",
                                                1,
                                                "truncated",
                                                false))
                                .build())) {
            when(protocol.callTool(any())).thenReturn(result);
            assertThrows(IllegalStateException.class, () -> client.querySql("SELECT 1"));
        }
        var failure = new IllegalStateException("transport closed");
        when(protocol.callTool(any())).thenThrow(failure);
        assertSame(
                failure,
                assertThrows(IllegalStateException.class, () -> client.querySql("SELECT 1")));
    }

    @Test
    void brokenDatabaseConnectionIsFatalRatherThanRepairableSqlError() {
        when(protocol.callTool(any()))
                .thenReturn(
                        McpSchema.CallToolResult.builder()
                                .isError(true)
                                .structuredContent(Map.of("errorType", "connection"))
                                .addTextContent("数据库连接中断")
                                .build());
        assertThrows(IllegalStateException.class, () -> client.querySql("SELECT 1"));
    }

    @Test
    void validatesAndReturnsRowsIncludingNulls() {
        var data =
                Map.<String, Object>of(
                        "columns",
                        List.of(Map.of("name", "x", "jdbcType", 12, "typeName", "VARCHAR")),
                        "rows",
                        List.of(Collections.singletonList(null)),
                        "rowCount",
                        1,
                        "truncated",
                        false);
        when(protocol.callTool(any()))
                .thenReturn(McpSchema.CallToolResult.builder().structuredContent(data).build());
        assertEquals(data, client.querySql("SELECT NULL"));
        verify(protocol)
                .callTool(new McpSchema.CallToolRequest("query_sql", Map.of("sql", "SELECT NULL")));
    }
}
