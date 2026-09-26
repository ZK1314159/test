package com.test.service.direct;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class DeepSeekClient {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL = "deepseek-flash";

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public DeepSeekClient(RestTemplateBuilder builder, @Value("${deepseek.api-key:}") String apiKey) {
        this(builder.setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(120)).build(), apiKey);
    }

    DeepSeekClient(RestTemplate restTemplate, String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    public String complete(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "未配置 deepseek.api-key");
        }

        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("messages", Collections.singletonList(message));
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(API_URL, HttpMethod.POST,
                    new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode responseBody = response.getBody();
            JsonNode content = responseBody == null ? null
                    : responseBody.path("choices").path(0).path("message").path("content");
            if (content == null || !content.isTextual() || content.asText().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DeepSeek 返回结果为空");
            }
            return content.asText();
        } catch (RestClientException e) {
            log.error("调用 DeepSeek API 失败，model={}", MODEL, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DeepSeek 请求失败", e);
        }
    }
}
