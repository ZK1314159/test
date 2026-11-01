package com.test.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.service.direct.AiClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConversionAnalysisServiceTest {
    private final ConversionUploadLogTool tool = mock(ConversionUploadLogTool.class);
    private final AiClient ai = mock(AiClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final LocalDateTime end = LocalDateTime.of(2026, 11, 1, 16, 0);

    @Test
    void sendsActualEvidenceToModelAndReturnsEvidenceWithAnalysis() throws Exception {
        Map<String, Object> summary = summary(100, 80, 0.8);
        when(tool.querySummary(end.minusHours(1), end)).thenReturn(summary);
        when(ai.complete(anyString())).thenReturn("成功率为80%，尚缺历史基线。");

        Map<String, Object> result = new ConversionAnalysisService(tool, ai, mapper)
                .analyze(end.minusHours(1), end);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai, times(1)).complete(prompt.capture());
        String text = prompt.getValue();
        String json = text.substring(text.indexOf("<query_result>\n") + "<query_result>\n".length(),
                text.indexOf("\n</query_result>"));
        // 比较 JSON 语义，避免 Java Long 与 JSON 解析出的 IntNode 类型差异。
        assertEquals(mapper.readTree(mapper.writeValueAsString(summary)), mapper.readTree(json));
        assertTrue(text.contains("缺少对比基线"));
        assertTrue(text.contains("初始化和处理中不等于失败"));
        assertSame(summary, result.get("summary"));
        assertEquals("成功率为80%，尚缺历史基线。", result.get("analysis"));
    }

    @Test
    void emptyWindowKeepsNullRateInModelInput() throws Exception {
        when(tool.querySummary(end.minusHours(1), end)).thenReturn(summary(0, 0, null));
        when(ai.complete(anyString())).thenReturn("无数据，无法判断。");
        new ConversionAnalysisService(tool, ai, mapper).analyze(end.minusHours(1), end);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).complete(prompt.capture());
        assertTrue(prompt.getValue().contains("\"successRate\":null"));
        assertTrue(prompt.getValue().contains("无数据，无法判断"));
    }

    @Test
    void queryFailureNeverBecomesAnEmptySuccessfulObservation() {
        RuntimeException failure = new DataAccessResourceFailureException("MySQL unavailable");
        when(tool.querySummary(end.minusHours(1), end)).thenThrow(failure);
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> new ConversionAnalysisService(tool, ai, mapper).analyze(end.minusHours(1), end)));
        verifyNoInteractions(ai);
    }

    @Test
    void modelFailureIsPropagatedRatherThanReturningInventedAnalysis() {
        when(tool.querySummary(end.minusHours(1), end)).thenReturn(summary(100, 80, 0.8));
        RuntimeException failure = new IllegalStateException("model unavailable");
        when(ai.complete(anyString())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> new ConversionAnalysisService(tool, ai, mapper).analyze(end.minusHours(1), end)));
    }

    private Map<String, Object> summary(long total, long success, Double rate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("start", end.minusHours(1).toString());
        result.put("end", end.toString());
        result.put("timeBasis", "created_at");
        result.put("total", total);
        result.put("success", success);
        result.put("failed", total == 0 ? 0L : 10L);
        result.put("initialized", total == 0 ? 0L : 5L);
        result.put("processing", total == 0 ? 0L : 5L);
        result.put("unknown", 0L);
        result.put("successRate", rate);
        return result;
    }
}
