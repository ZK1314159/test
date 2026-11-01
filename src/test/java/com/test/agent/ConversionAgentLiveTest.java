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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 显式开启才查询本机 MySQL、调用真实模型并导出逐轮明细。 */
@EnabledIfEnvironmentVariable(named = "AGENT_LOOP_LIVE_TEST", matches = "true")
class ConversionAgentLiveTest {
    @Test
    void modelChoosesQueriesAndSavesActualExchanges() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String runId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path traceDirectory = Paths.get("outputs", "agent-lesson-03-" + runId).toAbsolutePath();
        System.out.println("AGENT_TRACE_DIRECTORY=" + traceDirectory);
        // 关闭时导出；发生异常仍保存已有事件，保存异常由 Java 自动关联为 suppressed。
        try (AgentTrace trace = new AgentTrace(mapper, traceDirectory);
             AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
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
                // 每条只读查询自行完成，不在等待模型期间持有数据库事务。
                connection.setAutoCommit(true);
                JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                jdbc.setQueryTimeout(10);
                assertEquals("tdtracking", jdbc.queryForObject("SELECT DATABASE()", String.class));
                String configuredEnd = env.getProperty("AGENT_WINDOW_END");
                LocalDateTime anchor = configuredEnd == null
                        ? jdbc.queryForObject("SELECT NOW()", Timestamp.class).toLocalDateTime()
                        : LocalDateTime.parse(configuredEnd);
                String goal = "分析截至 " + anchor + " 最近一小时的回传是否正常，必要时与上一小时比较。"
                        + "请自行选择查询参数，说明证据和判断限制。";
                Map<String, Object> result = new ConversionAgent(new ConversionUploadLogTool(jdbc),
                        context.getBean(AiClient.class), mapper).run(goal, anchor, trace);
                assertTrue(((Number) result.get("toolCalls")).intValue() >= 1);
                assertFalse(((String) result.get("analysis")).trim().isEmpty());
                System.out.println("AGENT_LOOP_RESULT=" + mapper.writeValueAsString(result));
            }
        }
    }
}
