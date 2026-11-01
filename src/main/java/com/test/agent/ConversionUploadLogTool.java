package com.test.agent;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

/** 第一课：工具负责查询事实，后续再让模型决定何时调用它。 */
public class ConversionUploadLogTool {

    private static final String SUMMARY_SQL =
            "SELECT COUNT(*) AS total, "
            + "COALESCE(SUM(CASE WHEN upload_status = 20 THEN 1 ELSE 0 END), 0) AS success, "
            + "COALESCE(SUM(CASE WHEN upload_status = -1 THEN 1 ELSE 0 END), 0) AS failed, "
            + "COALESCE(SUM(CASE WHEN upload_status = 0 THEN 1 ELSE 0 END), 0) AS initialized, "
            + "COALESCE(SUM(CASE WHEN upload_status = 10 THEN 1 ELSE 0 END), 0) AS processing, "
            + "COALESCE(SUM(CASE WHEN upload_status IN (20, -1, 0, 10) THEN 0 ELSE 1 END), 0) AS unknown "
            + "FROM tdtracking.tracking_conversion_upload_log "
            + "WHERE created_at >= ? AND created_at < ?";

    private final JdbcTemplate jdbc;

    public ConversionUploadLogTool(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 使用数据库当地时间，左闭右开；第一版只允许查询至多一小时。 */
    public Map<String, Object> querySummary(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)
                || Duration.between(start, end).compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("时间范围必须大于 0 且不超过 1 小时");
        }

        // SQL 和表名固定，时间使用绑定参数；不接收模型生成的任意 SQL。
        Map<String, Object> counts = jdbc.queryForMap(SUMMARY_SQL,
                Timestamp.valueOf(start), Timestamp.valueOf(end));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("start", start.toString());
        result.put("end", end.toString());
        result.put("timeBasis", "created_at");
        for (String key : new String[]{"total", "success", "failed", "initialized", "processing", "unknown"}) {
            result.put(key, ((Number) counts.get(key)).longValue());
        }
        // 空窗口不计算成功率，避免将“没有数据”解释为“全部失败”。
        long total = (Long) result.get("total");
        result.put("successRate", total == 0 ? null : (Long) result.get("success") / (double) total);
        return result;
    }
}
