package com.test.service.direct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.util.Locale;
import java.util.function.Consumer;

@Service
@Slf4j
public class AiClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String provider;
    private final String model;
    private final String apiUrl;

    @Autowired
    public AiClient(RestTemplateBuilder builder,
                    @Value("${ai.provider:deepseek}") String provider,
                    @Value("${DEEPSEEK_API_KEY:}") String deepseekApiKey,
                    @Value("${ai.deepseek.model:deepseek-flash}") String deepseekModel,
                    @Value("${OPENAI_API_KEY:}") String openaiApiKey,
                    @Value("${ai.codex.model:gpt-6-luna}") String codexModel) {
        this(builder.setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(120)).build(), provider,
                deepseekApiKey, deepseekModel, openaiApiKey, codexModel);
    }

    AiClient(RestTemplate restTemplate, String apiKey) {
        this(restTemplate, "deepseek", apiKey, "deepseek-flash", "", "gpt-6-luna");
    }

    AiClient(RestTemplate restTemplate, String provider, String deepseekApiKey, String deepseekModel,
             String openaiApiKey, String codexModel) {
        this.restTemplate = restTemplate;
        this.provider = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!"deepseek".equals(this.provider) && !"codex".equals(this.provider)) {
            throw new IllegalArgumentException("ai.provider 仅支持 deepseek 或 codex");
        }
        boolean codex = "codex".equals(this.provider);
        this.apiKey = codex ? openaiApiKey : deepseekApiKey;
        this.model = codex ? codexModel : deepseekModel;
        this.apiUrl = codex ? "https://api.openai.com/v1/responses"
                : "https://api.deepseek.com/chat/completions";
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("所选 provider 的 model 不能为空");
        }
    }

    public String complete(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "未配置 " + ("codex".equals(provider) ? "OPENAI_API_KEY" : "DEEPSEEK_API_KEY"));
        }

        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        if ("codex".equals(provider)) {
            body.put("input", prompt);
            body.put("store", false);
        } else {
            body.put("messages", Collections.singletonList(message));
        }
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.POST,
                    new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode responseBody = response.getBody();
            if ("codex".equals(provider)) {
                return codexText(responseBody);
            }
            JsonNode content = responseBody == null ? null
                    : responseBody.path("choices").path(0).path("message").path("content");
            if (content == null || !content.isTextual() || content.asText().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DeepSeek 返回结果为空");
            }
            return content.asText();
        } catch (RestClientException e) {
            log.error("调用 AI API 失败，provider={}，model={}", provider, model, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, provider + " 请求失败", e);
        }
    }

    /** 第三课：保留工具调用等结构化输出；现有 complete(String) 的行为不变。 */
    public JsonNode respond(String instructions, ArrayNode input, ArrayNode tools,
                            Consumer<Map<String, Object>> recorder) {
        if (!"codex".equals(provider)) {
            throw new IllegalStateException("第三课工具循环仅实现 codex/Responses，请显式选择该提供方");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "未配置 OPENAI_API_KEY");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", instructions);
        body.put("input", input);
        body.put("tools", tools);
        body.put("tool_choice", "auto");
        body.put("parallel_tool_calls", false);
        body.put("store", false);
        body.put("stream", false);
        // 不依赖服务端保存对话；若模型返回推理状态，下一轮随 output 一起续传。
        body.put("include", Collections.singletonList("reasoning.encrypted_content"));

        Map<String, Object> requestEvent = new LinkedHashMap<>();
        requestEvent.put("kind", "model_request");
        requestEvent.put("method", "POST");
        requestEvent.put("url", apiUrl);
        requestEvent.put("body", body);
        recorder.accept(requestEvent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.POST,
                    new HttpEntity<>(body, headers), JsonNode.class);
            Map<String, Object> responseEvent = new LinkedHashMap<>();
            responseEvent.put("kind", "model_response");
            responseEvent.put("httpStatus", response.getStatusCodeValue());
            responseEvent.put("requestId", response.getHeaders().getFirst("x-request-id"));
            responseEvent.put("body", response.getBody());
            recorder.accept(responseEvent);
            JsonNode result = response.getBody();
            if (result == null || !"completed".equals(result.path("status").asText())
                    || !result.path("output").isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "工具调用未返回完整 output");
            }
            return result;
        } catch (RestClientException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("kind", "model_error");
            error.put("exceptionType", e.getClass().getName());
            recorder.accept(error);
            log.error("调用 Agent 模型失败，provider={}，model={}", provider, model, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent 模型请求失败", e);
        }
    }

    private String codexText(JsonNode body) {
        if (body == null || !"completed".equals(body.path("status").asText())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codex 未返回完整结果");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode item : body.path("output")) {
            if ("message".equals(item.path("type").asText())
                    && "assistant".equals(item.path("role").asText())) {
                for (JsonNode part : item.path("content")) {
                    if ("output_text".equals(part.path("type").asText()) && part.path("text").isTextual()) {
                        text.append(part.path("text").asText());
                    }
                }
            }
        }
        if (text.toString().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codex 返回结果为空");
        }
        return text.toString();
    }
}
