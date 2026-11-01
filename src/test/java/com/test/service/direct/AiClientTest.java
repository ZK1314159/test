package com.test.service.direct;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiClientTest {

    @Test
    void preservesCodexUpstreamFailureCause() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new AiClient(template, "codex", "", "deepseek-flash", "key", "gpt-6.1-sol")
                        .complete("你好"));
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(exception.getCause());
        server.verify();
    }

    @Test
    void rejectsEmptySelectedModel() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiClient(new RestTemplate(), "codex", "", "deepseek-flash", "key", " "));
    }

    @Test
    void sendsCodexResponsesRequestAndCombinesOnlyAssistantText() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer openai-test-key"))
                .andExpect(jsonPath("$.model", is("gpt-6.1-sol")))
                .andExpect(jsonPath("$.input", is("你好")))
                .andExpect(jsonPath("$.store", is(false)))
                .andRespond(withSuccess("{\"status\":\"completed\",\"output\":["
                        + "{\"type\":\"reasoning\",\"summary\":[]},"
                        + "{\"type\":\"message\",\"role\":\"assistant\",\"content\":["
                        + "{\"type\":\"output_text\",\"text\":\"你好\"},"
                        + "{\"type\":\"output_text\",\"text\":\"！\"}]}]}", MediaType.APPLICATION_JSON));
        assertEquals("你好！", new AiClient(template, "codex", "deepseek-key", "deepseek-flash",
                "openai-test-key", "gpt-6.1-sol").complete("你好"));
        server.verify();
    }

    @Test
    void selectedProviderUsesItsOwnConfiguredModelAndKey() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(header("Authorization", "Bearer deepseek-key"))
                .andExpect(jsonPath("$.model", is("custom-deepseek-model")))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}",
                        MediaType.APPLICATION_JSON));
        assertEquals("ok", new AiClient(template, "deepseek", "deepseek-key", "custom-deepseek-model",
                "openai-key", "gpt-6.1-sol").complete("你好"));
        server.verify();
    }

    @Test
    void rejectsMissingCodexKeyWithoutFallingBackToDeepSeek() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new AiClient(new RestTemplate(), "codex", "deepseek-key", "deepseek-flash",
                        "", "gpt-6.1-sol").complete("你好"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("未配置 OPENAI_API_KEY", exception.getReason());
    }

    @Test
    void rejectsUnknownProvider() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiClient(new RestTemplate(), "typo", "key", "model", "key", "model"));
    }

    @Test
    void rejectsIncompleteOrEmptyCodexResponses() {
        for (String body : new String[]{"{\"status\":\"completed\",\"output\":[]}",
                "{\"status\":\"incomplete\",\"output\":[{\"type\":\"message\",\"role\":\"assistant\","
                        + "\"content\":[{\"type\":\"output_text\",\"text\":\"partial\"}]}]}"}) {
            RestTemplate template = new RestTemplate();
            MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
            server.expect(requestTo("https://api.openai.com/v1/responses"))
                    .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> new AiClient(template, "codex", "", "deepseek-flash", "key", "gpt-6.1-sol")
                            .complete("你好"));
            assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
            server.verify();
        }
    }

    @Test
    void sendsPromptAndReturnsModelContent() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model", is("deepseek-flash")))
                .andExpect(jsonPath("$.messages[0].role", is("user")))
                .andExpect(jsonPath("$.messages[0].content", is("你好")))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"你好！\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertEquals("你好！", new AiClient(restTemplate, "test-key").complete("你好"));
        server.verify();
    }

    @Test
    void reportsUpstreamFailureAsBadGateway() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new AiClient(restTemplate, "test-key").complete("你好"));
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeCallingDeepSeek() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new AiClient(new RestTemplate(), "").complete("你好"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }
}
