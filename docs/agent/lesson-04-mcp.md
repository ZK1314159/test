# 第四课：提示词驱动的 SQL Agent 与 stdio MCP

模型根据用户问题自主决定查询步骤并生成完整 SQL。Java 维护对话上下文、执行 MCP 调用并传递结果，
不预设业务 SQL、统计口径或时间范围。早期课程的 `ConversionUploadLogTool` 和独立分析服务继续保留，
但不再接入 MCP。

## 运行链路

```text
用户问题 + 系统提示词 + MCP 工具定义
                 ↓
模型生成 query_sql({sql: 完整 SQL})
                 ↓
ConversionMcpClient → stdio → ConversionMcpServer → SqlQueryTool → 本机 MySQL
                 ↓
查询结果或错误 → function_call_output（使用原 call_id）→ 模型下一轮
                 ↓
继续探索/修正 SQL，或给出最终回答
```

`ConversionMcpClient` 启动独立 JVM，不持有 JDBC 连接。Server 只启动 MySQL 连接和 MCP，
不启动 Spring 应用、Redis 或模型；stdio 不监听 HTTP 端口，stdout 仅用于协议，日志写入 stderr。
stdin EOF 后服务退出，Client 关闭时回收子进程。

## 唯一工具：query_sql

```json
{"name":"query_sql","arguments":{"sql":"SHOW TABLES"}}
```

只接受一个非空 `sql` 字符串。原 `query_conversion_summary` 和 Client 的 `querySummary` 已移除。
Client 使用 `ConversionMcpClient.startLocal()` 启动，用 `querySql(String sql)` 查询。

成功结果示例：

```json
{
  "columns": [{"name":"total","jdbcType":-5,"typeName":"BIGINT"}],
  "rows": [[100]],
  "rowCount": 1,
  "truncated": false
}
```

`columns` 保留列名和 JDBC 类型；`rows` 使用按列顺序的数组，保留重复列名与 NULL。
日期时间转为数据库返回的字符串，二进制转为 Base64。`rowCount` 是已返回行数，不是数据库总数。
最多返回 200 行，额外读取一行判断 `truncated`；完整总量需由 SQL 聚合获得。
序列化结果超过 1 MiB 返回错误，不将不完整结果伪装为完整数据。单次 JDBC 查询超时为 10 秒。

SQL 使用 JSqlParser 4.9 解析为单条语句并检查语法树，再通过 PreparedStatement 执行解析后的 SQL。
支持 SELECT、JOIN、聚合、子查询、CTE，以及解析器支持的 SHOW、DESCRIBE、EXPLAIN。
常用结构探索包括 `SHOW DATABASES`、`SHOW TABLES`、`SHOW COLUMNS FROM 表名`、`DESCRIBE 表名`
和查询 `information_schema`。4.9 不支持的语法（例如 `SHOW CREATE TABLE`）会返回错误，可改用结构查询。

拒绝写入、DDL、多语句、锁定查询、SELECT INTO、EXPLAIN ANALYZE、变量赋值、未绑定问号参数、
MySQL 可执行注释及已知有副作用的函数。普通注释、字符串内的分号不按多语句拒绝。
连接保持只读、自动提交，单连接串行执行。数据库账户权限仍是权限边界，AST 校验不能替代权限控制。

URL 固定为本机 `127.0.0.1:3306/tdtracking`，时区 `Asia/Shanghai`；模型不能修改连接目标。
Server 复用 `application-dev.yml` 的 master 用户名密码，允许通过 `AGENT_MYSQL_USER` /
`AGENT_MYSQL_PASSWORD` 环境变量覆盖；不复用开发配置中的 URL。不要把秘密写入源码或命令参数。

## Agent 的提示词和多轮循环

系统提示词位于 `src/main/resources/prompts/sql-agent.txt`，以 UTF-8 加载，说明数据库背景、工具能力、
结构探索方法和结果限制；没有固定业务查询。`run(goal, anchor, trace)` 中的 `anchor` 仅补充相对时间背景。
用户问题作为单独 user 消息发送，不拼接到系统提示词中。

每次运行先通过 `tools/list` 发现并校验唯一允许工具 `query_sql`，再请求模型。
保留每轮模型完整输出（包括推理续传状态），执行返回的工具调用，并按原 `call_id` 回传结果。
同一轮有多个调用时串行执行。查询错误、只读校验失败和参数错误交给模型，模型可修正后继续查询。
SQL 错误反馈包含脱敏分类、SQLState 和数据库错误码，不返回驱动原始错误正文。

不限制模型轮数或工具调用次数，也不要求先查数据库才可回答。模型返回非空回答且没有工具调用时结束。
连接中断、协议/结果契约损坏、模型请求失败、未知工具或无效/重复 `call_id` 终止并报告；线程中断时退出。

返回顶层字段为 `observations`、`analysis`、`modelRequests`、`toolCalls`。
observation 包含调用标识、SQL 参数以及 `result` 或 `error`；`toolCalls` 统计实际发往 MCP 的工具调用，
包括执行错误，不包含在 Agent 本地拒绝的无效参数。所有错误也进入运行轨迹，不能当作空数据。

## 验证和运行

使用 JDK 17 与 IDEA 内置 Maven：

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -s /Users/kaiceng/.m2/settings.xml -B -ntp \
  '-Dtest=SqlQueryToolTest,ConversionMcpClientTest,ConversionMcpServerTest,ConversionMcpStdioTest,ConversionAgentTest,ConversionUploadLogToolTest,ConversionAnalysisServiceTest' test
```

覆盖通用 SQL 校验、数据类型和 NULL、重复列名、截断、结果大小、超时、工具错误与连接故障区分、
真实 stdio 子进程、EOF 退出，以及结构探索、错误修正、直接回答和超过旧调用上限的 Agent 对话。
模型测试使用模拟响应，不产生模型 API 用量。

显式开启真实本机 MySQL 只读联调：

```bash
AGENT_MYSQL_TEST=true JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -s /Users/kaiceng/.m2/settings.xml -B -ntp -Dtest=ConversionMcpMysqlLiveTest test
```

联调读取当前库、主机与版本，探索样例表结构，执行 CTE 聚合，检查错误查询后仍可继续查询，不新增或删除数据。

真实模型联调在 IDEA 运行 `ConversionAgentLiveTest`，设置 `AI_PROVIDER=codex`、
`AGENT_LOOP_LIVE_TEST=true`，可选 `AGENT_WINDOW_END=2026-11-01T16:00:00` 作为问题参考时间。
按 README-ai.md 通过安全存储注入模型密钥；此测试会产生真实模型 API 用量。
`startLocal()` 使用 IDEA/Maven 普通 classpath，不支持直接把 Boot 嵌套 JAR 当作普通 classpath。

运行轨迹记录 `mcp_tools_list`、`agent_round`、`mcp_tool_call`、`tool_result`、`agent_result`，
模型请求/响应仍由现有 AiClient 记录；只有 `agent_result` 表示正常交付最终回答。
`POST /ai/chat` 已接入同一个 SQL Agent，`prompt` 是用户问题入口；系统提示词仍由资源文件加载。
`SqlAgentChatService` 按请求创建并关闭 MCP Client，将最终 `analysis` 映射为兼容的 HTTP `result`，
并返回 `observations`、`modelRequests`、`toolCalls`。每个 HTTP 请求独立运行完整的模型与工具循环。

## 本次验证（2026-11-07）

使用 IDEA Maven 3.9.9、Temurin JDK 17.0.17，启用 `AGENT_MYSQL_TEST=true` 执行相关回归与 `package`：
60 项测试通过，0 失败、0 错误、0 跳过，`target/test-1.0.jar` 打包成功。
包含真实 stdio 子进程、错误 SQL 修正后的继续查询、超出旧轮数限制，以及本机 MySQL 9.5.0
的结构查询和 CTE 聚合；当前库确认为 `tdtracking`，未写入或删除数据库数据。
模型响应使用测试替身，未调用真实模型 API；未执行依赖真实 Redis、HBase 等服务的全量集成测试。

HTTP 接入验证：使用真实9000端口 POST `/ai/chat`，真实模型完成8轮请求、7次SQL查询并返回最终分析。
相关回归、本机MySQL与HTTP联调共64项测试通过，0失败、0错误、0跳过，`package`成功。
HTTP联调服务随测试结束关闭；日常使用需正常启动或重启应用。
