package com.test.agent.mcp;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.PrintStream;
import java.sql.DriverManager;

/** stdio 子进程入口：只启动本机 MySQL，不启动 Spring 应用、Redis 或模型。 */
public class ConversionMcpServerMain {
    public static void main(String[] args) throws Exception {
        // 保存协议输出；普通 println 和默认 ConsoleAppender 都只能写入 stderr。
        PrintStream protocolOutput = System.out;
        System.setOut(System.err);
        System.setProperty("logback.configurationFile", "logback-mcp.xml");

        StandardEnvironment env = new StandardEnvironment();
        new YamlPropertySourceLoader().load("local-config", new ClassPathResource("application-dev.yml"))
                .forEach(source -> env.getPropertySources().addLast(source));
        String prefix = "spring.datasource.dynamic.datasource.master.";
        String user = env.getProperty("AGENT_MYSQL_USER", env.getRequiredProperty(prefix + "username"));
        String password = env.getProperty("AGENT_MYSQL_PASSWORD", env.getRequiredProperty(prefix + "password"));
        // URL 固定为本机，不能通过模型参数或开发配置切换到生产。
        String url = "jdbc:mysql://127.0.0.1:3306/tdtracking?useSSL=false"
                + "&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
                + "&connectTimeout=5000&socketTimeout=15000";
        try (var connection = DriverManager.getConnection(url, user, password)) {
            connection.setReadOnly(true);
            connection.setAutoCommit(true);
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.setQueryTimeout(10);
            new ConversionMcpServer(new SqlQueryTool(jdbc))
                    .serve(System.in, protocolOutput);
        }
    }
}
