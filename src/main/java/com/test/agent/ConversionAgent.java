package com.test.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.test.agent.mcp.ConversionMcpClient;
import com.test.agent.mcp.ConversionMcpServer;
import com.test.service.direct.AiClient;

import io.modelcontextprotocol.spec.McpSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CancellationException;

/** 模型自主生成 SQL，Java 维护对话并通过 stdio MCP 执行查询。 */
public class ConversionAgent {
    private static final Logger log = LoggerFactory.getLogger(ConversionAgent.class);
    private final ConversionMcpClient tool;
    private final AiClient client;
    private final ObjectMapper mapper;
    private final String prompt;

    public ConversionAgent(ConversionMcpClient tool, AiClient client, ObjectMapper mapper) {
        this.tool = tool;
        this.client = client;
        this.mapper = mapper;
        try (var input = new ClassPathResource("prompts/sql-agent.txt").getInputStream()) {
            prompt = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("SQL Agent 提示词加载失败", e);
            throw new IllegalStateException("SQL Agent 提示词加载失败", e);
        }
    }

    public Map<String, Object> run(String goal, LocalDateTime anchor, AgentTrace trace)
            throws JsonProcessingException {
        if (goal == null || goal.isBlank() || anchor == null) {
            throw new IllegalArgumentException("目标和时间锚点不能为空");
        }
        checkInterrupted();
        List<McpSchema.Tool> discovered = tool.listTools();
        ArrayNode definitions = toolDefinitions(discovered);
        trace.record(event("mcp_tools_list", "tools", discovered));
        ArrayNode input = mapper.createArrayNode();
        input.addObject().put("role", "user").put("content", goal);
        String instructions = prompt + "\n当前参考时间：" + anchor + "（Asia/Shanghai），仅用于理解相对时间，不限制查询窗口。";
        List<Map<String, Object>> observations = new ArrayList<>();
        Set<String> callIds = new HashSet<>();
        int executed = 0;
        int round = 0;
        while (true) {
            checkInterrupted();
            round++;
            trace.record(event("agent_round", "round", round));
            JsonNode response =
                    client.respond(
                            instructions, input.deepCopy(), definitions.deepCopy(), trace::record);
            checkInterrupted();
            JsonNode output = response.path("output");
            if (!output.isArray()) throw new IllegalStateException("模型 output 必须是数组");
            // 包含推理状态的完整模型输出继续传给下一轮。
            input.addAll((ArrayNode) output);
            List<JsonNode> calls = new ArrayList<>();
            for (JsonNode item : output) {
                if ("function_call".equals(item.path("type").asText())) calls.add(item);
            }
            if (calls.isEmpty()) {
                String analysis = assistantText(output);
                if (analysis.isBlank()) throw new IllegalStateException("模型未返回工具调用或最终回答");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("observations", observations);
                result.put("analysis", analysis);
                result.put("modelRequests", round);
                result.put("toolCalls", executed);
                trace.record(event("agent_result", "result", result));
                return result;
            }
            // 先核对整批调用身份，避免一批中的协议错误导致部分工具已执行。
            for (JsonNode call : calls) {
                if (!call.path("call_id").isTextual()
                        || call.path("call_id").asText().isBlank()
                        || !callIds.add(call.path("call_id").asText())
                        || !ConversionMcpServer.TOOL_NAME.equals(call.path("name").asText())) {
                    throw new IllegalArgumentException("未知工具或无效/重复 call_id");
                }
            }
            for (JsonNode call : calls) {
                checkInterrupted();
                String callId = call.path("call_id").asText();
                Map<String, Object> observation = new LinkedHashMap<>();
                observation.put("callId", callId);
                observation.put("name", ConversionMcpServer.TOOL_NAME);
                observation.put("arguments", call.path("arguments"));
                Map<String, Object> feedback = null;
                JsonNode args = null;
                try {
                    args = parseArguments(call);
                } catch (IllegalArgumentException | JsonProcessingException e) {
                    log.warn("SQL Agent 参数无效，callId={}", callId, e);
                    feedback = Map.of("isError", true, "error", "工具参数必须是 JSON 对象，仅包含一个非空 sql 字符串");
                    observation.put("error", feedback);
                }
                if (args != null) {
                    observation.put("arguments", args);
                    trace.record(
                            event(
                                    "mcp_tool_call",
                                    "request",
                                    Map.of(
                                            "callId",
                                            callId,
                                            "name",
                                            ConversionMcpServer.TOOL_NAME,
                                            "arguments",
                                            args)));
                    executed++;
                    try {
                        feedback = tool.querySql(args.path("sql").asText());
                        observation.put("result", feedback);
                    } catch (ConversionMcpClient.SqlToolException e) {
                        log.warn(
                                "SQL Agent 查询失败，callId={}，tool={}",
                                callId,
                                ConversionMcpServer.TOOL_NAME,
                                e);
                        feedback = Map.of("isError", true, "error", e.getMessage());
                        observation.put("error", feedback);
                    }
                }
                checkInterrupted();
                input.addObject()
                        .put("type", "function_call_output")
                        .put("call_id", callId)
                        .put("output", mapper.writeValueAsString(feedback));
                observations.add(observation);
                trace.record(event("tool_result", "observation", observation));
            }
        }
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted())
            throw new CancellationException("SQL Agent 已中断");
    }

    private ArrayNode toolDefinitions(List<McpSchema.Tool> discovered) {
        List<McpSchema.Tool> allowed =
                discovered.stream()
                        .filter(candidate -> ConversionMcpServer.TOOL_NAME.equals(candidate.name()))
                        .toList();
        if (allowed.size() != 1) throw new IllegalStateException("MCP 必须提供唯一的 query_sql 工具");
        McpSchema.Tool definition = allowed.get(0);
        JsonNode schema = mapper.valueToTree(definition.inputSchema());
        if (!"object".equals(schema.path("type").asText())
                || !schema.path("additionalProperties").isBoolean()
                || schema.path("additionalProperties").asBoolean()
                || !schema.path("properties").isObject()
                || schema.path("properties").size() != 1
                || !"string".equals(schema.path("properties").path("sql").path("type").asText())
                || !schema.path("required").isArray()
                || schema.path("required").size() != 1
                || !"sql".equals(schema.path("required").get(0).asText())) {
            throw new IllegalStateException("MCP SQL 工具参数契约不兼容");
        }
        ArrayNode tools = mapper.createArrayNode();
        ObjectNode function =
                tools.addObject()
                        .put("type", "function")
                        .put("name", definition.name())
                        .put("strict", true)
                        .put("description", definition.description());
        function.set("parameters", schema);
        return tools;
    }

    private JsonNode parseArguments(JsonNode call) throws JsonProcessingException {
        if (!call.path("arguments").isTextual())
            throw new IllegalArgumentException("arguments 必须是 JSON 字符串");
        JsonNode args =
                mapper.reader()
                        .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .with(
                                com.fasterxml.jackson.core.JsonParser.Feature
                                        .STRICT_DUPLICATE_DETECTION)
                        .readTree(call.path("arguments").asText());
        if (args == null
                || !args.isObject()
                || args.size() != 1
                || !args.path("sql").isTextual()
                || args.path("sql").asText().isBlank()) {
            throw new IllegalArgumentException("参数只允许非空 sql 字符串");
        }
        return args;
    }

    private String assistantText(JsonNode output) {
        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            if ("message".equals(item.path("type").asText())
                    && "assistant".equals(item.path("role").asText())) {
                for (JsonNode part : item.path("content")) {
                    if ("output_text".equals(part.path("type").asText()))
                        text.append(part.path("text").asText());
                }
            }
        }
        return text.toString();
    }

    private Map<String, Object> event(String kind, String key, Object value) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("kind", kind);
        event.put(key, value);
        return event;
    }
}
