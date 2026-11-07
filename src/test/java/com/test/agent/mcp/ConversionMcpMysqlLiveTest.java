package com.test.agent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/** 显式开启后，真实启动生产代码的 MCP 子进程，只读查询本机 MySQL，不调用模型。 */
@EnabledIfEnvironmentVariable(named = "AGENT_MYSQL_TEST", matches = "true")
class ConversionMcpMysqlLiveTest {
    @Test
    @Timeout(40)
    void discoverAndQueryActualLocalMysqlThroughStdio() throws Exception {
        String configured = System.getenv("AGENT_WINDOW_END");
        LocalDateTime anchor = configured == null ? LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                : LocalDateTime.parse(configured);
        try (var client = ConversionMcpClient.startLocal(anchor)) {
            assertEquals("query_conversion_summary", client.listTools().get(0).name());
            var summary = client.querySummary(anchor.minusHours(1), anchor);
            assertNotNull(summary.get("total"));
            assertEquals("created_at", summary.get("timeBasis"));
            System.out.println("AGENT_MCP_MYSQL_RESULT=" + new ObjectMapper().writeValueAsString(summary));
        }
    }
}
