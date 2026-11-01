package com.test.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.service.direct.AiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 显式开启后，查询真实本机 MySQL 并调用一次配置的模型，会产生 API 用量。 */
@EnabledIfEnvironmentVariable(named = "AGENT_ANALYSIS_LIVE_TEST", matches = "true")
class ConversionAnalysisLiveTest {

    @Test
    void queryMysqlAndAskModelToAnalyze() throws Exception {
        // 运行环境：只装配模型客户端，不启动完整应用和其他基础设施。
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            ConfigurableEnvironment env = context.getEnvironment();
            new YamlPropertySourceLoader().load("local-config", new ClassPathResource("application-dev.yml"))
                    .forEach(source -> env.getPropertySources().addLast(source));
            context.registerBean(RestTemplateBuilder.class, () -> new RestTemplateBuilder());
            context.register(AiClient.class);
            context.refresh();

            String prefix = "spring.datasource.dynamic.datasource.master.";
            String user = env.getProperty("AGENT_MYSQL_USER", env.getRequiredProperty(prefix + "username"));
            String password = env.getProperty("AGENT_MYSQL_PASSWORD", env.getRequiredProperty(prefix + "password"));
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
                            ? jdbc.queryForObject("SELECT NOW()", Timestamp.class).toLocalDateTime()
                            : LocalDateTime.parse(configuredEnd);

                    // 组装工具、模型和编排类，执行第二课的完整固定流程。
                    ObjectMapper mapper = new ObjectMapper();
                    ConversionAnalysisService service = new ConversionAnalysisService(
                            new ConversionUploadLogTool(jdbc), context.getBean(AiClient.class), mapper);
                    Map<String, Object> result = service.analyze(end.minusHours(1), end);
                    assertNotNull(result.get("summary"));
                    assertFalse(((String) result.get("analysis")).trim().isEmpty());
                    // 自动断言只证明链路有返回，分析内容仍需结合 summary 人工核对。
                    System.out.println("AGENT_ANALYSIS_RESULT="
                            + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
                } finally {
                    connection.rollback();
                }
            }
        }
    }
}
