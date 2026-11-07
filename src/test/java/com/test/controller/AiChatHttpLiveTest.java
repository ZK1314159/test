package com.test.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.agent.SqlAgentChatService;
import com.test.service.direct.AiClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/** 显式开启才在9000端口启动真实HTTP服务，调用真实模型和MySQL；结束后关闭本测试服务。 */
@EnabledIfEnvironmentVariable(named = "AGENT_HTTP_LIVE_TEST", matches = "true")
class AiChatHttpLiveTest {
    @Test
    void promptDrivesSqlAgentOverActualHttp() throws Exception {
        try (var context = new AnnotationConfigServletWebServerApplicationContext()) {
            new YamlPropertySourceLoader()
                    .load("local-config", new ClassPathResource("application-dev.yml"))
                    .forEach(
                            source ->
                                    context.getEnvironment().getPropertySources().addLast(source));
            context.register(HttpConfiguration.class);
            context.refresh();
            var http =
                    new RestTemplateBuilder()
                            .setConnectTimeout(Duration.ofSeconds(5))
                            .setReadTimeout(Duration.ofMinutes(10))
                            .build();
            JsonNode response =
                    http.postForObject(
                            "http://localhost:9000/ai/chat",
                            Map.of(
                                    "prompt",
                                    "请自行探索本机回传数据结构，对比2026-11-01"
                                        + " 14:00至15:00和15:00至16:00的回传记录、错误标记及状态分布，并用中文说明变化和判断限制。"),
                            JsonNode.class);
            assertNotNull(response);
            assertFalse(response.path("result").asText().isBlank());
            assertTrue(response.path("toolCalls").asInt() > 0);
            assertTrue(response.path("observations").isArray());
            String output = System.getProperty("agent.http.result");
            if (output != null) {
                Path path = Path.of(output);
                Files.createDirectories(path.getParent());
                Files.writeString(
                        path,
                        new ObjectMapper()
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(response));
            }
            System.out.println("AGENT_HTTP_RESULT=" + response);
        }
    }

    @Configuration
    @EnableWebMvc
    @Import({AiController.class, AiClient.class, SqlAgentChatService.class})
    static class HttpConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder();
        }

        @Bean
        TomcatServletWebServerFactory webServerFactory() {
            return new TomcatServletWebServerFactory(9000);
        }

        @Bean
        ServletRegistrationBean<DispatcherServlet> dispatcherServlet(
                org.springframework.web.context.WebApplicationContext context) {
            return new ServletRegistrationBean<>(new DispatcherServlet(context), "/");
        }
    }
}
