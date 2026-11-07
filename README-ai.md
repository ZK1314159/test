# AI 对话接口

回传分析 Agent 已通过 stdio MCP 发现和调用 MySQL 查询工具，详见
[第四课：stdio MCP](docs/agent/lesson-04-mcp.md)。该入口与下方文本聊天接口独立。

`POST /ai/chat` 接收 `{"prompt":"你好"}`，返回 `{"result":"模型回复"}`。
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

切回 DeepSeek 时设置 `AI_PROVIDER=deepseek`，并提供 `DEEPSEEK_API_KEY`。

```bash
curl -X POST http://localhost:9000/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"你好"}'
```

## 验证

`AiClientTest` 和 `AiControllerTest` 不调用外部 API。
`AiLiveTest` 默认关闭；显式设置 `AI_LIVE_TEST=true` 并注入密钥后，验证真实模型调用，
包括读取 YAML 配置、Spring 注入、`/ai/chat` 请求处理和结果解析，不依赖数据库或 Redis。
该用例会产生实际 API 用量。
