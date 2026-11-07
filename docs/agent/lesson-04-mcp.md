# 第四课：通过 stdio MCP 发现和调用 MySQL 查询工具

本课复用 `ConversionUploadLogTool.querySummary()` 的 SQL 和统计口径，将它放到独立 JVM
提供服务。`ConversionAgent` 不再直接调用 JDBC，而是通过 MCP Client 发现工具并调用。
第一、二课保留直接调用方式；第三课文档中的历史运行证据保留，当前 Agent 入口已切换为本课实现。

## 整体调用链

```text
Agent JVM                                         Server JVM
ConversionAgent.run
  ├─ ConversionMcpClient.listTools ── tools/list ──→ 工具声明
  │       ↓
  │   inputSchema → 模型 function 的 parameters
  ├─ AiClient.respond：模型选择工具及 start/end
  └─ ConversionMcpClient.querySummary ─ tools/call → ConversionMcpServer.handle
                                                     ↓ 时间/授权校验
                                                   querySummary → JdbcTemplate → MySQL
                 ←────────── structuredContent ──────┘
      ↓ 验证统计结果、匹配原模型 call_id
  function_call_output → 下一轮模型请求 → 最终回答
```

stdio 不是 HTTP，不监听端口。Client 用 `java -cp ... ConversionMcpServerMain` 启动子进程，
SDK 在父子进程的 stdin/stdout 中传输 JSON-RPC。两个 JVM 不共享 Java 对象。
Server 不需要模型密钥，Client 不直接持有 JDBC 连接。

## 按这个顺序读代码

| 文件 | 重点 |
| --- | --- |
| `src/main/java/com/test/agent/mcp/ConversionMcpServerMain.java` | 保存协议 stdout、初始化本机 MySQL、进入服务循环 |
| `src/main/java/com/test/agent/mcp/ConversionMcpServer.java` | `toolDefinition()` 是菜单，`specification()` 绑定执行函数，`handle()` 真正查询 |
| `src/main/java/com/test/agent/mcp/ConversionMcpClient.java` | 初始化连接，分页 `listTools()`，`callTool()`，检查返回证据，关闭进程 |
| `src/main/java/com/test/agent/ConversionAgent.java` | 开始运行时发现工具、转换声明，模型提出请求后调用 MCP |
| `src/test/java/com/test/agent/ConversionAgentLiveTest.java` | 完整运行入口，由 try-with-resources 管理客户端 |

### 注册与调用是怎样对应起来的

Server 中的 `SyncToolSpecification` 绑定两件事：

```java
new McpServerFeatures.SyncToolSpecification(
    toolDefinition(),                  // name、description、inputSchema
    (exchange, request) -> handle(request) // 收到 tools/call 时执行
);
```

Client 发送的工具名必须与声明一致：

```java
client.callTool(new McpSchema.CallToolRequest(
    "query_conversion_summary",
    Map.of("start", start.toString(), "end", end.toString())
));
```

参数和结果在进程间经历 JSON 序列化；Java 方法和数据库密码不在调用消息里传递。
SDK 负责初始化、协议编解码、分发、协议请求 ID 和响应匹配。
模型 `call_id` 仍由 Agent 保留，不能与 MCP JSON-RPC 请求 ID 混为一谈。

### tools/list 如何进入模型上下文

每次 `run()` 开始时调用 `listTools()`；客户端读取所有分页并限制重复游标和最大页数。
Agent 只选择允许的 `query_conversion_summary`，要求工具唯一且输入仍为必填的 start/end 字符串。
未知工具不进入模型工具集，契约不兼容则在请求模型之前停止。

转换关系：`name → name`、`description → description`、`inputSchema → parameters`，
再加现有模型接口需要的 `type=function`、`strict=true`。
工具名称是本应用的白名单；描述和 Schema 来自实际发现结果，不再维护另一份硬编码声明。

## 查询边界与失败行为

- 固定表、固定 SELECT、时间参数绑定，不开放任意 SQL。
- Agent 和 Server 都限制锚点前两小时、单次大于零且不超过一小时；锚点由可信启动配置提供，
  不接受模型请求修改。Agent 仍限制最多四次模型请求、三次工具执行。
- Server URL 固定为本机 `127.0.0.1:3306/tdtracking`，时间按当地 `Asia/Shanghai` 解释，
  JDBC 只读、自动提交、10 秒查询超时，并配置连接和 socket 超时。
- 只读连接标记不等于账号已具备数据库级只读权限；实际账号授权需独立管理。
- Server 按序执行查询，共享连接不跨查询保持事务；不同窗口不保证同一数据库快照。
- 普通输出和日志写 stderr，保留的 stdout 只供协议使用。stdin EOF 后退出；Client 关闭时回收子进程。
- Client 子进程环境仅传必要变量，不继承模型 API Key。凭据只经进程环境或已有本机开发配置读取。
- 数据库失败返回 `isError=true`，Server 记录完整异常；Client 停止分析，不返回假空数据。
  Client 校验时间窗口、统计口径、非负整数、状态计数合计与成功率。
- 空窗口仍成功返回：全部计数为零，`successRate=null`。
- 为保持原有行为，本课不添加自动纠错重试；超时、协议故障或证据异常都会停止本次分析。

## 依赖与运行方式

项目已升级 Java 17。本课使用 MCP Java SDK 1.1.5 的 `mcp-core` 和 `mcp-json-jackson2`。
Jackson BOM 调整为 2.20.1、Reactor BOM 为 2024.0.0，与 SDK 对齐；保留 Boot 2.7 的日志体系。
Jackson YAML 单独保留原来的 2.13.5，避免新版 YAML 适配器要求的 SnakeYAML API 与 Boot 2.7 冲突。
增加独立 main 后，在 Boot 插件中明确默认入口仍为 `com.test.TestApplication`。

`startLocal()` 面向 IDEA / Maven 的普通 classpath。它不支持直接把 Spring Boot 嵌套 JAR
当成普通 classpath；不要用 `java -cp target/test-1.0.jar ...` 启动 MCP。
本课不新增 Web 接口，`/ai/chat` 继续是原来的文本接口。

### 先运行不需要数据库或模型的协议测试

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -s /Users/kaiceng/.m2/settings.xml -B -ntp \
  '-Dtest=ConversionMcpClientTest,ConversionMcpServerTest,ConversionMcpStdioTest,ConversionAgentTest' test
```

`ConversionMcpStdioTest` 真正启动另一个 JVM，仅 JDBC 使用测试替身；模型使用模拟响应。
覆盖工具发现、正常查询、空数据、拒绝越界、数据库失败、未知工具、额外参数、超时、EOF 退出，
以及 Agent 把同一个模型 call_id 与 MCP 查询结果对应起来。

### 只运行真实本机 MySQL + MCP，不调用模型

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
AGENT_MYSQL_TEST=true AGENT_WINDOW_END=2026-11-01T16:00:00 \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -s /Users/kaiceng/.m2/settings.xml -B -ntp \
  -Dtest=ConversionMcpMysqlLiveTest test
```

查看 `AGENT_MCP_MYSQL_RESULT=`。本测试不新增或删除数据。
Server 复用 `application-dev.yml` 的 master 用户名密码，但不使用其中的 URL；
可通过 `AGENT_MYSQL_USER` / `AGENT_MYSQL_PASSWORD` 覆盖，不把秘密写入命令或源码。

### 真实模型 + MCP + MySQL

在 IDEA 运行 `ConversionAgentLiveTest`，使用 JDK 17，设置：

```text
AI_PROVIDER=codex
AGENT_LOOP_LIVE_TEST=true
AGENT_WINDOW_END=2026-11-01T16:00:00
```

按 `README-ai.md` 通过安全存储注入模型密钥；运行会产生实际模型 API 用量。
测试仍在独立 `outputs/agent-lesson-03-<runId>/` 下生成明细，沿用既有目录约定。
新增 `mcp_tools_list`、`mcp_tool_call` 事件；它们是应用层发现结果与调用请求，
不是逐字节协议抓包，不伪造或记录 SDK 内部 JSON-RPC ID。
只有最终出现 `agent_result` 才表示分析完成。

## 本次验证（2026-11-07）

使用 IDEA 内置 Maven、Temurin 17.0.17 执行以下回归与打包：

```bash
AGENT_MYSQL_TEST=true AGENT_WINDOW_END=2026-11-01T16:00:00 \
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -s /Users/kaiceng/.m2/settings.xml -B -ntp \
  '-Dtest=AiClientTest,AiControllerTest,ConversionAnalysisServiceTest,ConversionAgentTest,ConversionUploadLogToolTest,BeanUtilTest,*CompatibilityTest,ConversionMcp*Test' \
  package
```

50 项测试通过，0 失败、0 错误、0 跳过，`target/test-1.0.jar` 打包成功。
包含实际应用组件启动、Feign、Dubbo、Redisson YAML 与缓存编解码等既有兼容测试。
真实 MCP 子进程查询本机样例窗口 2026-11-01 15:00～16:00：total=100、success=80、
failed=10、initialized=5、processing=5、unknown=0、successRate=0.8。
本次仅 SELECT，没有写入或删除样例；未调用真实模型 API，模型循环使用模拟响应验证。
未执行依赖真实 Redis、HBase 等外部环境的全部项目集成测试。

同日北京时间 22:00 又实际运行 `ConversionAgentLiveTest`：真实 `gpt-6-luna` 模型请求三次，
均返回 HTTP 200，MCP 工具执行两次，测试通过。模型先查询 15:00～16:00（成功率 80%），
再自行查询 14:00～15:00（成功率 98%），第三轮依据两份查询证据输出下降 18 个百分点的结论，
同时说明判断限制。已逐项核对 tools/list 声明与模型工具定义、调用参数、call_id 及下一轮
function_call_output 的统计 JSON 完全一致。服务端报告总用量为 2133 Token，未估算费用。

## 参考

- [MCP 工具发现和调用](https://modelcontextprotocol.io/specification/2025-11-25/server/tools)
- [stdio 传输约定](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)
- [固定版本 SDK 源码](https://github.com/modelcontextprotocol/java-sdk/tree/v1.1.5)
