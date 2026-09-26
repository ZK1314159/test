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

class DeepSeekClientTest {

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

        assertEquals("你好！", new DeepSeekClient(restTemplate, "test-key").complete("你好"));
        server.verify();
    }

    @Test
    void reportsUpstreamFailureAsBadGateway() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new DeepSeekClient(restTemplate, "test-key").complete("你好"));
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeCallingDeepSeek() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new DeepSeekClient(new RestTemplate(), "").complete("你好"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }
}
