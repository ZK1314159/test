# AI 对话接口

SQL Agent 通过 stdio MCP 的 `query_sql(sql)` 查询本机 MySQL：模型自主探索表结构、生成 SQL，
根据每轮结果继续查询；SQL 错误反馈给模型修正，最终回答时结束。不限制模型轮数和工具调用次数，
允许直接回答。系统提示词位于 `src/main/resources/prompts/sql-agent.txt`。
详见[第四课：stdio SQL MCP](docs/agent/lesson-04-mcp.md)。

`POST http://localhost:9000/ai/chat` 是 SQL Agent 的用户问题入口。请求中的 `prompt` 作为独立 user 消息，
与资源文件中的系统提示词一起驱动模型自主生成 SQL、多轮查询并最终回答。

```json
{"prompt":"对比2026-11-01 14:00至15:00和15:00至16:00的回传情况，说明变化和判断限制"}
```

响应保留原来的 `result` 字符串，并增加实际工具调用记录及统计：

```json
{"result":"模型最终回答","observations":[],"modelRequests":1,"toolCalls":0}
```

每个请求独立维护内部多轮上下文，不跨 HTTP 请求保存对话。请求结束后关闭 MCP 子进程；
SQL 错误可交给模型修正，不可恢复的连接、协议或模型故障返回错误，保留原始异常链。
当前工具循环使用 `AI_PROVIDER=codex`；`AiClient.complete()` 仍供独立文本调用使用。
请从 IDEA 或 Maven 普通 classpath 启动应用，MCP 子进程启动暂不支持 Boot 嵌套 JAR classpath。
原 `/deepseek/chat` 入口已更名为 `/ai/chat`。

## 配置切换

在 `application-dev.yml` 中修改 `ai.provider`，或在启动时设置环境变量：

| 环境变量 | 用途 | 开发环境默认值 |
| --- | --- | --- |
| `AI_PROVIDER` | `deepseek` 或 `codex` | `codex` |
| `CODEX_MODEL` | Codex 模型名，对应 `ai.codex.model` | `gpt-6-luna` |
| `DEEPSEEK_MODEL` | DeepSeek 模型名，对应 `ai.deepseek.model` | `deepseek-flash` |
| `OPENAI_API_KEY` | Codex/OpenAI 密钥 | 无 |
| `DEEPSEEK_API_KEY` | DeepSeek 密钥 | 无 |

配置在应用启动时生效，更改后需要重启。只读取所选提供方的密钥，缺失时返回 503；
上游请求失败、空结果或 Codex 响应未完成时返回 502，不自动切换提供方。
Codex 使用 OpenAI Responses API，DeepSeek 使用 Chat Completions API。

## macOS 密钥

OpenAI 密钥存放在登录钥匙串：service `api.openai.com`，account `codex-api`。
应用本身只读取 `OPENAI_API_KEY` 环境变量，不会直接访问钥匙串。macOS 可以通过
`security` 命令读取钥匙串，再把密钥注入启动进程。在启动应用的同一终端执行，
不要把密钥复制到配置文件或提交到 Git：

```bash
export OPENAI_API_KEY="$(security find-generic-password -s api.openai.com -a codex-api -w)"
export AI_PROVIDER=codex
# 使用现有的应用启动方式；进程需要继承以上环境变量。
```

已经打开的 IDEA 不会自动继承终端新设置的环境变量；IDEA 的 Environment variables
也不会执行 `$(security ...)` 这样的 shell 命令。使用上述方式时，需在该终端直接启动
应用（例如运行 IDEA 内置 Maven 的 `spring-boot:run`）。首次读取钥匙串时，macOS
可能要求授权访问；读取失败应停止启动，不用空密钥继续运行。

独立文本调用可通过 `AI_PROVIDER=deepseek` 和 `DEEPSEEK_API_KEY` 使用 DeepSeek；当前 `/ai/chat` SQL Agent 需要 `codex`。

```bash
curl -X POST http://localhost:9000/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"你好"}'
```

## 验证

`AiClientTest` 和 `AiControllerTest` 不调用外部 API。
`AiLiveTest` 默认关闭；显式设置 `AI_LIVE_TEST=true` 并注入密钥后，验证真实模型调用，
包括读取 YAML 配置、Spring 注入、`/ai/chat` 请求处理和结果解析，需要本机 MySQL MCP，不依赖 Redis。
该用例会产生实际 API 用量。

`AiChatHttpLiveTest` 通过 `AGENT_HTTP_LIVE_TEST=true` 显式开启，在空闲的9000端口启动真实HTTP服务，
向 `/ai/chat` POST 用户问题，经过真实模型、MCP与MySQL后检查响应；测试结束自动关闭该测试服务。
可用 `-Dagent.http.result=/绝对路径/response.json` 导出实际响应。
