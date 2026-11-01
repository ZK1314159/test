package com.test.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 只启动真实 MySQL 连接，不启动完整 Spring 应用、Redis 或模型。 */
@EnabledIfEnvironmentVariable(named = "AGENT_MYSQL_TEST", matches = "true")
class ConversionUploadLogToolLiveTest {

    @Test
    void queryRealLocalMysql() throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        new YamlPropertySourceLoader().load("local-config", new ClassPathResource("application-dev.yml"))
                .forEach(source -> env.getPropertySources().addLast(source));
        String prefix = "spring.datasource.dynamic.datasource.master.";
        String user = env.getProperty("AGENT_MYSQL_USER", env.getRequiredProperty(prefix + "username"));
        String password = env.getProperty("AGENT_MYSQL_PASSWORD", env.getRequiredProperty(prefix + "password"));
        // 教学运行器固定连接本机指定数据库；不继承配置中的远程 URL。
        String url = "jdbc:mysql://127.0.0.1:3306/tdtracking?useSSL=false"
                + "&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            try {
                JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                jdbc.setQueryTimeout(10);
                assertEquals("tdtracking", jdbc.queryForObject("SELECT DATABASE()", String.class));
                String configuredEnd = env.getProperty("AGENT_WINDOW_END");
                LocalDateTime end = configuredEnd == null
                        ? jdbc.queryForObject("SELECT NOW()", java.sql.Timestamp.class).toLocalDateTime()
                        : LocalDateTime.parse(configuredEnd);
                Map<String, Object> result = new ConversionUploadLogTool(jdbc)
                        .querySummary(end.minusHours(1), end);
                long accounted = 0;
                for (String key : new String[]{"success", "failed", "initialized", "processing", "unknown"}) {
                    accounted += (Long) result.get(key);
                }
                assertEquals(result.get("total"), accounted);
                System.out.println("AGENT_TOOL_RESULT=" + new ObjectMapper().writeValueAsString(result));
            } finally {
                connection.rollback();
            }
        }
    }
}
