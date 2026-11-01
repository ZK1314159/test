package com.test.agent;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConversionUploadLogToolTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ConversionUploadLogTool tool = new ConversionUploadLogTool(jdbc);
    private final LocalDateTime end = LocalDateTime.of(2026, 11, 1, 16, 0);

    @Test
    void reportsCurrentStatusIncludingPendingAndUnknown() {
        when(jdbc.queryForMap(anyString(), any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(counts(100, 80, 10, 5, 4, 1));
        Map<String, Object> result = tool.querySummary(end.minusHours(1), end);
        assertEquals(0.8, result.get("successRate"));
        assertEquals(5L, result.get("initialized"));
        assertEquals(4L, result.get("processing"));
        assertEquals(1L, result.get("unknown"));
        assertEquals("created_at", result.get("timeBasis"));
    }

    @Test
    void emptyWindowHasNoSuccessRate() {
        when(jdbc.queryForMap(anyString(), any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(counts(0, 0, 0, 0, 0, 0));
        Map<String, Object> result = tool.querySummary(end.minusHours(1), end);
        assertEquals(0L, result.get("total"));
        assertNull(result.get("successRate"));
    }

    @Test
    void invalidWindowsNeverReachDatabase() {
        assertThrows(IllegalArgumentException.class, () -> tool.querySummary(null, end));
        assertThrows(IllegalArgumentException.class, () -> tool.querySummary(end, null));
        assertThrows(IllegalArgumentException.class, () -> tool.querySummary(end, end));
        assertThrows(IllegalArgumentException.class, () -> tool.querySummary(end, end.minusHours(1)));
        assertThrows(IllegalArgumentException.class, () -> tool.querySummary(end.minusHours(2), end));
        verifyNoInteractions(jdbc);
    }

    private Map<String, Object> counts(long... values) {
        Map<String, Object> counts = new LinkedHashMap<>();
        String[] keys = {"total", "success", "failed", "initialized", "processing", "unknown"};
        for (int i = 0; i < keys.length; i++) {
            counts.put(keys[i], values[i]);
        }
        return counts;
    }
}
