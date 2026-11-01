package com.test.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

/** 只记录请求体、响应体与工具事件；不接收凭据或完整 HTTP 请求头。 */
public class AgentTrace implements AutoCloseable {
    private final ObjectMapper mapper;
    private final ObjectNode document;
    private final Path directory;

    public AgentTrace(ObjectMapper mapper) {
        this(mapper, null);
    }

    public AgentTrace(ObjectMapper mapper, Path directory) {
        this.mapper = mapper;
        this.directory = directory;
        document = mapper.createObjectNode();
        document.put("provenance", "运行时直接记录的请求/响应 JSON，不是事后重建或原始网络报文");
        document.put("startedAt", now());
        document.putArray("events");
    }

    public void record(Map<String, Object> event) {
        // 立即复制：后续对话历史会增长，不能让已记录的第一轮输入跟着变化。
        ObjectNode snapshot = mapper.valueToTree(event);
        snapshot.put("recordedAt", now());
        document.withArray("events").add(snapshot);
    }

    public ObjectNode document() {
        return document.deepCopy();
    }

    @Override
    public void close() throws IOException {
        // 配合 try-with-resources：保存失败不会覆盖原始模型/数据库异常。
        if (directory != null) {
            save(directory);
        }
    }

    public void save(Path directory) throws IOException {
        Files.createDirectories(directory);
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        Files.write(directory.resolve("call-details.json"), json.getBytes(StandardCharsets.UTF_8));
        StringBuilder readable = new StringBuilder("第三课：真实 Agent 调用明细\n\n")
                .append("运行时记录完整请求/响应 JSON、工具参数和结果；不包含认证请求头。\n")
                .append("model_request 是发出的请求体；model_response 是收到的响应体及白名单元数据。\n")
                .append("tool_result 是 Java 查询 MySQL 后交回模型的结果。agent_result 是最终输出。\n")
                .append("只有出现 agent_result 才表示循环正常完成；否则查看最后一轮事件与测试错误。\n")
                .append("reasoning.encrypted_content 如存在，是用于续传的加密状态，并非可读思维过程。\n\n");
        for (com.fasterxml.jackson.databind.JsonNode event : document.withArray("events")) {
            readable.append("===== ").append(event.path("kind").asText("event")).append(" =====\n")
                    .append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event)).append("\n\n");
        }
        Files.write(directory.resolve("call-details.txt"), readable.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String now() {
        return OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).toString();
    }
}
