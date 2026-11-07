package com.test.agent.mcp;

import com.test.agent.ConversionUploadLogTool;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/** 真实 stdio 子进程，仅替换 JDBC 外部协作者；不连接数据库、不调用模型。 */
public class ConversionMcpTestServer {
    public static void main(String[] args) throws Exception {
        var protocolOutput = System.out;
        System.setOut(System.err);
        System.setProperty("logback.configurationFile", "logback-mcp.xml");
        System.out.println("fixture startup log belongs to stderr");
        JdbcTemplate jdbc = new JdbcTemplate() {
            @Override
            public Map<String, Object> queryForMap(String sql, Object... parameters) {
                if (!sql.contains("WHERE created_at >= ? AND created_at < ?")
                        || parameters.length != 2 || !(parameters[0] instanceof Timestamp)) {
                    throw new AssertionError("查询必须使用绑定参数和固定时间条件");
                }
                if (args.length > 0 && "failure".equals(args[0])) {
                    throw new DataAccessResourceFailureException("fixture database unavailable");
                }
                if (args.length > 0 && "slow".equals(args[0])) {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("fixture query interrupted", e);
                    }
                }
                if (((Timestamp) parameters[0]).toLocalDateTime().getHour() == 14) {
                    return counts(0, 0, 0, 0, 0, 0);
                }
                return counts(100, 80, 10, 5, 4, 1);
            }
        };
        new ConversionMcpServer(new ConversionUploadLogTool(jdbc), LocalDateTime.parse("2026-11-01T16:00:00"))
                .serve(System.in, protocolOutput);
    }

    private static Map<String, Object> counts(long total, long success, long failed,
                                               long initialized, long processing, long unknown) {
        return Map.of("total", total, "success", success, "failed", failed,
                "initialized", initialized, "processing", processing, "unknown", unknown);
    }
}
