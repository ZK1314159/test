package com.test.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.agent.mcp.ConversionMcpClient;
import com.test.agent.mcp.ConversionMcpServer;
import com.test.service.direct.AiClient;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

class SqlAgentChatServiceTest {
    final ObjectMapper mapper = new ObjectMapper();
    final AiClient model = mock(AiClient.class);
    final ConversionMcpClient mcp = mock(ConversionMcpClient.class);

    SqlAgentChatService service() {
        return new SqlAgentChatService(model, mapper) {
            @Override
            protected ConversionMcpClient openClient() {
                return mcp;
            }
        };
    }

    @Test
    void runsAgentAndPreservesResultFieldAndClosesMcp() throws Exception {
        when(mcp.listTools()).thenReturn(List.of(ConversionMcpServer.toolDefinition()));
        when(mcp.querySql("SELECT 1"))
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
        var call = mapper.createObjectNode();
        call.putArray("output")
                .addObject()
                .put("type", "function_call")
                .put("name", "query_sql")
                .put("call_id", "c1")
                .put("arguments", "{\"sql\":\"SELECT 1\"}");
        var answer = mapper.createObjectNode();
        answer.putArray("output")
                .addObject()
                .put("type", "message")
                .put("role", "assistant")
                .putArray("content")
                .addObject()
                .put("type", "output_text")
                .put("text", "查询完成");
        when(model.respond(anyString(), any(), any(), any())).thenReturn(call, answer);
        var result = service().chat("查询测试数据");
        assertEquals("查询完成", result.get("result"));
        assertEquals(2, result.get("modelRequests"));
        assertEquals(1, result.get("toolCalls"));
        assertFalse(result.containsKey("analysis"));
        assertEquals(1, ((List<?>) result.get("observations")).size());
        verify(mcp).querySql("SELECT 1");
        verify(mcp).close();
        verify(model, never()).complete(anyString());
    }

    @Test
    void preservesProviderStatusAndClosesMcpOnFailure() {
        when(mcp.listTools()).thenReturn(List.of(ConversionMcpServer.toolDefinition()));
        var error = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "未配置模型密钥");
        when(model.respond(anyString(), any(), any(), any())).thenThrow(error);
        assertSame(
                error, assertThrows(ResponseStatusException.class, () -> service().chat("查询数据")));
        verify(mcp).close();
    }

    @Test
    void protocolFailureBecomes502WithOriginalCauseAndClosesMcp() {
        var error = new IllegalStateException("连接中断");
        when(mcp.listTools()).thenThrow(error);
        var actual = assertThrows(ResponseStatusException.class, () -> service().chat("查询数据"));
        assertEquals(HttpStatus.BAD_GATEWAY, actual.getStatus());
        assertSame(error, actual.getCause());
        verify(mcp).close();
    }
}
