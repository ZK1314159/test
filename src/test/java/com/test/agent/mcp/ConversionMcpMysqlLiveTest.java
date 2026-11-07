package com.test.agent.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 显式开启后启动真实 MCP 子进程，只读查询本机 MySQL，不调用模型。 */
@EnabledIfEnvironmentVariable(named = "AGENT_MYSQL_TEST", matches = "true")
class ConversionMcpMysqlLiveTest {
    @Test
    @Timeout(40)
    void queriesActualMysqlThroughStdio() throws Exception {
        try (var client = ConversionMcpClient.startLocal()) {
            assertEquals(1, client.listTools().size());
            assertEquals("query_sql", client.listTools().get(0).name());
            var identity =
                    client.querySql(
                            "SELECT DATABASE() AS db, @@hostname AS host, VERSION() AS version");
            assertEquals(
                    "tdtracking",
                    ((java.util.List<?>) ((java.util.List<?>) identity.get("rows")).get(0)).get(0));
            System.out.println(
                    "AGENT_MCP_MYSQL_RESULT=" + new ObjectMapper().writeValueAsString(identity));
            assertNotNull(client.querySql("SHOW TABLES").get("columns"));
            assertNotNull(
                    client.querySql("DESCRIBE tracking_conversion_upload_log").get("columns"));
            assertNotNull(
                    client.querySql(
                                    "WITH cte_rows AS (SELECT upload_status FROM"
                                        + " tracking_conversion_upload_log) SELECT upload_status,"
                                        + " COUNT(*) AS n FROM cte_rows GROUP BY upload_status")
                            .get("rows"));
            assertThrows(
                    ConversionMcpClient.SqlToolException.class,
                    () ->
                            client.querySql(
                                    "SELECT no_such_column FROM tracking_conversion_upload_log"));
            assertEquals(1, client.querySql("SELECT 1").get("rowCount"));
        }
    }
}
