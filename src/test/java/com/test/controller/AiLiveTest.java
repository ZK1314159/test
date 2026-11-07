package com.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.agent.SqlAgentChatService;
import com.test.service.direct.AiClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 显式开启才调用真实模型；使用项目配置与环境变量，通过本机 MySQL MCP 启动 Agent，不启动其他基础设施。 */
@EnabledIfEnvironmentVariable(named = "AI_LIVE_TEST", matches = "true")
class AiLiveTest {

    @Test
    void configuredChatEndpointCallsRealProvider() throws Exception {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            new YamlPropertySourceLoader()
                    .load("ai-config", new ClassPathResource("application-dev.yml"))
                    .forEach(
                            source ->
                                    context.getEnvironment().getPropertySources().addLast(source));
            context.registerBean(RestTemplateBuilder.class, () -> new RestTemplateBuilder());
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.register(AiClient.class, SqlAgentChatService.class, AiController.class);
            context.refresh();
            MockMvc mvc =
                    MockMvcBuilders.standaloneSetup(context.getBean(AiController.class)).build();
            mvc.perform(
                            post("/ai/chat")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"prompt\":\"Reply with exactly: CODEX_OK\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("CODEX_OK"));
        }
    }
}
