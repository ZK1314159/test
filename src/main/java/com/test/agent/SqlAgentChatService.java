package com.test.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.agent.mcp.ConversionMcpClient;
import com.test.service.direct.AiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** HTTP 请求与 SQL Agent 的适配层；每个请求独立维护对话和 MCP 生命周期。 */
@Service
public class SqlAgentChatService {
    private static final Logger log = LoggerFactory.getLogger(SqlAgentChatService.class);
    private final AiClient model;
    private final ObjectMapper mapper;

    public SqlAgentChatService(AiClient model, ObjectMapper mapper) {
        this.model = model;
        this.mapper = mapper;
    }

    public Map<String, Object> chat(String prompt) {
        String requestId = UUID.randomUUID().toString();
        try (ConversionMcpClient mcp = openClient();
                AgentTrace trace = new AgentTrace(mapper)) {
            Map<String, Object> result =
                    new ConversionAgent(mcp, model, mapper)
                            .run(prompt, LocalDateTime.now(ZoneId.of("Asia/Shanghai")), trace);
            Map<String, Object> response = new LinkedHashMap<>();
            // 保留原 HTTP 接口的 result 字符串，同时提供实际查询记录与计数。
            response.put("result", result.get("analysis"));
            response.put("observations", result.get("observations"));
            response.put("modelRequests", result.get("modelRequests"));
            response.put("toolCalls", result.get("toolCalls"));
            return response;
        } catch (Exception e) {
            log.error("SQL Agent HTTP 分析失败，requestId={}", requestId, e);
            if (e instanceof ResponseStatusException status) throw status;
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SQL Agent 分析失败，请查看服务端日志", e);
        }
    }

    protected ConversionMcpClient openClient() {
        return ConversionMcpClient.startLocal();
    }
}
