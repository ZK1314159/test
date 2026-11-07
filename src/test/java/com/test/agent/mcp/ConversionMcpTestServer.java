package com.test.agent.mcp;

import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.*;

/** 真实 stdio 子进程，仅替换数据库外部协作者。 */
public class ConversionMcpTestServer {
    public static void main(String[] args) throws Exception {
        var output = System.out;
        System.setOut(System.err);
        System.setProperty("logback.configurationFile", "logback-mcp.xml");
        System.out.println("fixture startup log belongs to stderr");
        SqlQueryTool query =
                new SqlQueryTool(new JdbcTemplate()) {
                    @Override
                    public Map<String, Object> querySql(String sql) {
                        validateSql(sql);
                        if (sql.contains("missing")
                                || args.length > 0 && "failure".equals(args[0])) {
                            throw new UncategorizedSQLException(
                                    "fixture",
                                    sql,
                                    new SQLException("fixture bad column", "42S22", 1054));
                        }
                        if (args.length > 0 && "slow".equals(args[0])) {
                            try {
                                Thread.sleep(4000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("fixture interrupted", e);
                            }
                        }
                        boolean empty = sql.contains("WHERE 1=0");
                        return Map.of(
                                "columns",
                                List.of(
                                        Map.of(
                                                "name",
                                                "value",
                                                "jdbcType",
                                                4,
                                                "typeName",
                                                "INTEGER")),
                                "rows",
                                empty ? List.of() : List.of(List.of(1)),
                                "rowCount",
                                empty ? 0 : 1,
                                "truncated",
                                false);
                    }
                };
        new ConversionMcpServer(query).serve(System.in, output);
    }
}
