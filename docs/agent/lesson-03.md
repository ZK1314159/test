# 第三课：让模型选择并请求调用工具

本课目标：让模型根据任务提出查询请求，由 Java 执行真实 MySQL 查询，再把结果交回模型。
模型可以继续查询，也可以结束并回答。本课仍使用第一课的 `querySummary`，没有引入 MCP 或新框架。

## 与第二课相比，控制权发生了什么变化

第二课由程序固定执行 `querySummary → complete`。
第三课先把任务和工具声明发给模型；Java 此时还没有查询数据。

```text
任务 + 工具说明 → 模型
                  ↓
        返回 function_call（工具名、参数、call_id）
                  ↓
        Java 校验并执行 querySummary
                  ↓
        function_call_output（同一个 call_id、结果）
                  ↓
        模型继续选择工具，或输出最终分析
```

“模型调用工具”不意味着模型自己连接数据库。模型生成结构化调用请求，程序才是实际执行者。
实现依据：[OpenAI 官方 Function calling 文档](https://developers.openai.com/api/docs/guides/function-calling)。

## 代码阅读顺序

| 文件 | 职责 |
| --- | --- |
| `src/main/java/com/test/agent/ConversionAgent.java` | 本课主线：上下文、循环、执行工具、回传、结束 |
| `src/main/java/com/test/service/direct/AiClient.java` 的 `respond` | 发送工具声明，保留结构化响应 |
| `src/main/java/com/test/agent/AgentTrace.java` | 运行时记录请求、响应与工具事件 |
| `src/test/java/com/test/agent/ConversionAgentLiveTest.java` | 组装依赖，执行真实链路，保存明细 |
| `src/test/java/com/test/agent/ConversionAgentTest.java` | 模拟模型输出，验证循环与边界 |

下面对应 `ConversionAgent` 中的编号注释阅读。

## 第 1 段：把任务放进初始上下文

```java
ArrayNode input = mapper.createArrayNode();
input.addObject().put("role", "user").put("content", goal);
```

`input` 保存本次运行的对话历史。一开始只有用户目标，没有预先查询的统计结果。
`instructions` 则定义 Agent 的角色、时间锚点、允许窗口、统计口径和回答边界。
它告诉模型可查询锚点前两小时，每次至多一小时，根据需要参考上一小时。

锚点由运行器提供；演示固定为 `2026-11-01T16:00:00`，以便读取第一课保留的数据。
模型自己选择 `start/end`，Java 仍会检查选择是否在允许范围内。

## 准备工具声明：告诉模型“有哪些能力”

`toolDefinitions()` 构造类似下面的声明：

```json
{
  "type": "function",
  "name": "query_conversion_summary",
  "description": "只读查询指定时间窗口创建的回传记录的当前状态统计；每次至多一小时，可用于目标窗口或对比窗口。",
  "strict": true,
  "parameters": {
    "type": "object",
    "properties": {
      "start": {"type": "string", "description": "开始时间，包含，ISO本地时间，如2026-11-01T15:00:00"},
      "end": {"type": "string", "description": "结束时间，不包含，ISO本地时间，如2026-11-01T16:00:00"}
    },
    "required": ["start", "end"],
    "additionalProperties": false
  }
}
```

`name` 用于找到 Java 实现；`description` 帮模型判断什么时候用；`parameters` 说明如何传参。
声明中没有 SQL、数据库账号或密码。本课只有一个工具，模型可以选择调用或结束、查询哪个窗口，
还没有加入多个不同查询工具之间的选择。

`strict` 约束参数结构，但时间格式、范围和调用权限仍由 Java 检查。

## 第 2 段：让模型决定下一步

```java
for (int round = 1; round <= 4; round++) {
    JsonNode response = client.respond(
            instructions, input.deepCopy(), toolDefinitions(), trace::record);
    // 后续代码处理这轮响应
}
```

循环表示每拿到一次新观察，就给模型一次重新选择行动的机会。
`input.deepCopy()` 固定本轮上下文，避免后面追加历史改变已经交出的输入。
`trace::record` 把记录函数交给客户端，以便在实际发送和收到响应时保存数据。

`AiClient.respond()` 的关键请求字段是：

```text
instructions          固定角色与行为边界
input                 本次累计的对话、模型输出和工具结果
tools                 可调用的工具声明
tool_choice = auto    由模型决定调用工具还是回答
parallel_tool_calls = false  本课按顺序执行
store = false         不依赖服务端保存对话
```

原 `complete()` 提取文本即可；`respond()` 必须保留完整结构化响应，
否则 `function_call` 中的名称、参数和 `call_id` 会丢失。
第三课只实现项目 `codex` 提供方对应的 Responses 协议；若配置为 DeepSeek，会明确拒绝，
不会偷偷切换提供方。原 `/ai/chat` 和第二课的调用方式继续使用原逻辑。

## 第 3 段：把模型输出保留到下一轮

```java
input.addAll((ArrayNode) output);
```

响应可能包含 `function_call`、普通消息和推理状态等不同类型的项，不能只保存文字。
本课把全部 output 项加入历史，并请求 `reasoning.encrypted_content`，
使不在服务端存储的调用也能续传模型返回的加密状态。

加密状态是用于继续请求的不透明数据，不是可阅读的模型思维过程。

## 第 4 段：判断模型是要回答，还是要行动

```java
if (calls.isEmpty()) {
    String analysis = assistantText(output);
    // 核对已有查询证据与非空回答，然后返回 observations + analysis
}
```

如果没有工具调用请求，就提取最终回答。程序要求本次运行至少执行过一次查询，
避免模型在没有任何工具证据的情况下直接交付“正常”的结论。
这只是最低证据门槛，并不自动保证自然语言分析正确；仍应核对数字与结论。

## 第 5 段：检查请求，再执行 Java 工具

```java
JsonNode args = parseArguments(call);
LocalDateTime start = LocalDateTime.parse(args.get("start").asText());
LocalDateTime end = LocalDateTime.parse(args.get("end").asText());
// 核对时间范围、工具名和 call_id 后
Map<String, Object> summary = tool.querySummary(start, end);
```

这一步才访问 MySQL。模型提供的 `arguments` 本身是包含 JSON 的字符串，需要先解析。
程序只允许工具名 `query_conversion_summary`，参数只能是 `start/end`；
不能从模型文字中提取任意 SQL 去执行，也不使用反射调用任意 Java 方法。

非法参数、未知工具、重复 `call_id` 或越界时间都会停止运行。
当前没有自动纠错重试分支，这是为了让第一版循环和失败行为容易观察。

## 第 6 段：把工具结果交回模型

```java
ObjectNode toolOutput = mapper.createObjectNode()
        .put("type", "function_call_output")
        .put("call_id", callId)
        .put("output", mapper.writeValueAsString(summary));
input.add(toolOutput);
```

`call_id` 把结果与模型先前提出的那一次请求对应起来。
Java 执行完查询并不代表模型已经看到了结果；必须在下一轮请求中把这项结果发过去。
模型拿到新证据后，才有机会继续查询上一小时或者输出最终分析。

## 为什么要限制次数

本课最多请求模型四次、执行数据库工具三次。
如果第四轮仍要求查工具，就停止并报错；不会把“预算耗尽”伪装成分析完成。
`tool_choice` 保持 `auto`，程序不强制模型发出某个函数调用。

## 实际调用明细如何保存

`AgentTrace.record()` 在事件发生时立即复制 JSON；`AgentTrace.close()` 在退出时保存：

```text
outputs/agent-lesson-03-<时间>-<随机标记>/
  call-details.txt   按事件顺序显示完整请求、响应与查询结果
  call-details.json  同一份数据的结构化版本
```

每次运行创建独立目录，不覆盖先前明细。包含以下事件：

| kind | 含义 |
| --- | --- |
| agent_round | 开始第几轮模型请求 |
| model_request | 实际发送的请求体，含 instructions、input 和 tools |
| model_response | 实际收到的完整响应 JSON、HTTP 状态和 x-request-id |
| tool_result | 模型所选参数与 Java 查询的统计结果 |
| model_error | HTTP 调用失败的异常类型；错误不冒充正常响应 |
| agent_result | 已完成的最终分析及工具证据 |

成功响应 JSON 原样保留其内容，包括响应 id、model 和 usage（若服务端返回）。
这里保存的是应用层 JSON，不是逐字节网络抓包。认证请求头和 Cookie 不进入明细。
只记录服务端实际返回的用量，不估算费用或补造未返回的字段。

即使中途出错也保存已有事件；`try-with-resources` 会保留原始异常，
记录保存失败时会作为 suppressed 异常关联。只有看到 `agent_result` 才代表循环完成。

## 自己运行

使用完整 Corretto JDK 8 和 IDEA 内置 Maven，以兼容本项目已有依赖。
在项目目录执行；密钥只注入本次子 shell：

```bash
(
  export JAVA_HOME="$(/usr/libexec/java_home -v 1.8.0_462)"
  export OPENAI_API_KEY
  OPENAI_API_KEY="$(security find-generic-password -s api.openai.com -a codex-api -w)" || exit 1
  [ -n "$OPENAI_API_KEY" ] || exit 1

  AI_PROVIDER=codex \
  CODEX_MODEL=gpt-6-luna \
  AGENT_LOOP_LIVE_TEST=true \
  AGENT_WINDOW_END=2026-11-01T16:00:00 \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
    -B -ntp -Dtest=ConversionAgentLiveTest test
)
```

会产生实际 API 用量，一次运行最多四次模型请求。不要开启 shell `set -x`。
入口固定连接本机 `127.0.0.1:3306/tdtracking`，复用前两课的本地账号密码。
每次查询以只读、自动提交方式完成，等待模型期间不保持数据库事务；多次查询不承诺同一数据库快照。

读取控制台 `AGENT_TRACE_DIRECTORY=`，按顺序打开明细文件，重点查看：

1. 第一轮 input 中只有任务，没有预先查询的统计。
2. 第一轮 output 是否包含模型生成的 function_call，参数是什么。
3. 下一轮 input 是否包含匹配 call_id 的 function_call_output。
4. 模型是否继续查询，然后在没有 function_call 的一轮给出最终文本。

本次使用第一课保留的合成样例：14～15 点成功率 98%，15～16 点为 80%。
模型应根据实际查到的窗口引用数字，不能据此推断生产环境，也不能编造业务阈值或故障根因。

## 本次已验证的真实运行

2026-11-01 17:25（北京时间）实际执行本课，保留完整明细于：

```text
outputs/agent-lesson-03-20261101-172532-2b1d2967/
  run-summary.txt   本次逐轮概览及最终分析
  call-details.txt  完整请求/响应与工具事件，易读版
  call-details.json 同一份运行时记录的结构化版本
```

真实模型选择的步骤：

1. 第一轮请求查询 `2026-11-01T15:00:00` 到 `2026-11-01T16:00:00`，实际得到成功率 0.8。
2. 第二轮请求查询 `2026-11-01T14:00:00` 到 `2026-11-01T15:00:00`，实际得到成功率 0.98。
3. 第三轮停止调用工具，指出成功率下降 18 个百分点，同时说明缺少业务阈值和更多明细。

三次模型响应均为 HTTP 200，服务端返回模型均为 `gpt-6-luna`。
服务端返回各轮总 Token 数分别为 520、670、1104，合计 2294；未据此估算费用。
已核对每轮输入与此前模型输出、工具结果一致，`call_id` 与查询结果一一对应。

验证结果：29 个定向单元/模拟 HTTP 测试通过，2 个真实 MySQL/模型链路测试通过，
共 31 个测试，0 失败、0 跳过。真实 Agent 运行共 3 次模型请求、2 次数据库工具执行。
本课没有新增、修改或删除数据库样例。

检查了工具白名单、非法/越界参数、重复 call_id、轮数限制、异常传播、
历史与推理状态续传，以及明细快照和认证信息排除。没有发现需要阻塞的后端问题。
自然语言结论只核对本次样例，不代表对所有后续模型回答的自动质量保证。
