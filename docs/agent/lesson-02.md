# 第二课：把查询结果交给模型分析

目标：复用第一课的工具查询本机 MySQL，把统计结果交给现有 `AiClient`，返回模型解释。
这一课由程序固定安排执行顺序，只调用模型一次，不让模型生成 SQL 或主动调用工具。

```text
指定时间窗口
  → ConversionUploadLogTool.querySummary
  → 统计 Map
  → JSON + 分析任务和规则
  → AiClient.complete
  → 模型文本
  → 返回 summary + analysis
```

## 先打开哪些文件

1. `src/main/java/com/test/agent/ConversionAnalysisService.java`：本课核心，按注释 1～5 阅读。
2. `src/test/java/com/test/agent/ConversionAnalysisLiveTest.java`：组装依赖并实际运行本课。
3. `src/main/java/com/test/service/direct/AiClient.java`：项目已有的模型 HTTP 客户端，本课复用它。
4. `src/test/java/com/test/agent/ConversionAnalysisServiceTest.java`：用替身验证编排和错误路径。

第一课的查询工具、SQL 和状态定义保持原样。本课不需要新依赖或 MCP 服务。

## 准备：让编排类拿到工具和模型

```java
public ConversionAnalysisService(ConversionUploadLogTool tool, AiClient ai, ObjectMapper mapper) {
    this.tool = tool;
    this.ai = ai;
    this.mapper = mapper;
}
```

`tool` 提供查询能力，`ai` 提供模型调用能力，`mapper` 负责 JSON 格式转换。
通过构造函数传入它们，使各部分职责清楚，也方便单元测试替换数据库和模型。
目前这个类是普通 Java 类，运行器负责 `new`，暂时不增加 Spring 组件或 HTTP 入口。

## 第 1 段：调用工具，得到观察结果

```java
Map<String, Object> summary = tool.querySummary(start, end);
```

在 Agent 流程中，这叫获取观察结果：从环境中获得事实。
数据来自真实 MySQL。模型此时尚未参与，不负责计数，也没有直接访问数据库。
查询异常会直接向上传递，不能把连接失败当成“查到零条数据”。

当前时间窗口仍由我们指定。下一课才会研究如何让模型提出工具调用请求。

## 第 2 段：把观察结果转换成模型输入

```java
String evidenceJson = mapper.writeValueAsString(summary);
```

模型 API 接收文本或消息，不接收 Java Map 对象本身。JSON 清楚保留字段名、数字和 `null`。
例如当前教学窗口中的 `successRate` 是 `0.8`，而空窗口中是 `null`，两者含义不同。

序列化只是在转换格式，不是在调用模型或做分析。
这里使用第一课工具产生的汇总字段，不发送数据库密码、用户标识或原始回传报文。

## 第 3 段：定义分析任务与判断边界

代码中的 `prompt` 由固定规则与本次 JSON 组成：

```text
你要完成的分析任务
统计口径与字段定义
哪些结论缺少证据、不能直接下结论
回答采用什么结构
<query_result>
本次查询的 JSON
</query_result>
```

这就是提示词：把目标、领域知识、约束和证据放进模型本次能看到的上下文。
使用标签是为了让规则和数据的边界清楚，不会自动获得更高的消息权限。
本课复用 `AiClient.complete(String)`，整体仍是一段输入文本。

代码中的规则分别解决以下问题：

| 规则 | 原因 |
| --- | --- |
| 按 created_at 归组，观察查询时当前状态 | 防止把记录数量说成发送尝试次数或历史时刻状态 |
| 初始化和处理中不等于失败 | 防止把所有未成功记录归为失败 |
| 0.8 表示 80%，空窗口成功率为 null | 防止解释比例和无数据状态时出错 |
| 缺少基线、阈值、渠道与错误明细 | 单个窗口无法证明波动、系统整体正常或具体根因 |
| 按结论、数据依据、判断限制、建议查询回答 | 让解释容易阅读，也方便核对是否有证据 |

提示词可以引导行为，但不能保证模型每次都遵守。这一课返回解释供人阅读，
尚未把模型判断当成程序可执行的告警条件，也没有自动验证全部自然语言结论。

提示词结构参考：[OpenAI 官方提示词文档](https://developers.openai.com/api/docs/guides/prompt-engineering#message-formatting-with-markdown-and-xml)。

## 第 4 段：调用模型，让模型解释证据

```java
String analysis = ai.complete(prompt);
```

这是本课真正发生模型调用的位置。项目已有客户端负责读取配置、选择提供方、
发送 HTTP 请求和提取响应文本。当前默认配置是 `codex` 提供方和 `gpt-6-luna` 模型，
配置仍可按 `README-ai.md` 的方式覆盖，本课没有修改提供方实现。

模型只看到提示词中的统计数据；数据库地址、账号和密码没有作为工具权限交给它。
模型在本次调用中也不能再查询渠道或错误详情。

如果模型 API 调用失败，沿用客户端原有异常处理；编排类不伪造一份“正常”的分析结果。
现有客户端遇到 HTTP 异常会记录堆栈并保留 cause，编排类不捕获或丢弃该异常。

## 第 5 段：同时交付事实和解释

```java
Map<String, Object> result = new LinkedHashMap<>();
result.put("summary", summary);
result.put("analysis", analysis);
return result;
```

`summary` 是工具返回的证据，`analysis` 是模型生成的文本。把两者一起返回，
你可以核对模型引用的数字是否正确，以及结论有没有超出证据。

这里没有解析模型文本并执行动作。后续如果要自动告警，再讨论结构化输出和规则验证。

## 实际运行

运行入口是 `ConversionAnalysisLiveTest`，默认关闭，显式设置
`AGENT_ANALYSIS_LIVE_TEST=true` 才访问真实模型，会产生 API 用量。
本机连接固定为 `127.0.0.1:3306/tdtracking`，复用第一课的本地数据库凭据。

推荐从终端运行以下命令：钥匙串密钥仅进入本次子 shell 的环境，退出后不留在当前终端。
不要开 shell 的 `set -x`，也不要把密钥复制进代码或文档。

```bash
(
  export JAVA_HOME="$(/usr/libexec/java_home -v 1.8.0_462)"
  export OPENAI_API_KEY
  OPENAI_API_KEY="$(security find-generic-password -s api.openai.com -a codex-api -w)" || exit 1
  [ -n "$OPENAI_API_KEY" ] || exit 1

  AI_PROVIDER=codex \
  AGENT_ANALYSIS_LIVE_TEST=true \
  AGENT_WINDOW_END=2026-11-01T16:00:00 \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
    -B -ntp -Dtest=ConversionAnalysisLiveTest test
)
```

在本项目目录运行。使用完整 Corretto JDK 8，是为了兼容项目已有旧版 Lombok。
运行器不启动整个应用或 Redis，只装配模型客户端并连接本地 MySQL。

控制台 `AGENT_ANALYSIS_RESULT=` 后会同时展示查询统计和真实模型分析。
固定时间用于复现第一课教学数据；去掉 `AGENT_WINDOW_END` 才会查询运行时滚动的一小时。
样例是合成数据，因此解释只能作为教学分析，不能代表生产环境状态。

## 本次验证结果

2026-11-01 使用 IDEA 内置 Maven 和 Corretto 8 执行以下定向测试：

```text
ConversionAnalysisServiceTest         4 通过
ConversionUploadLogToolTest           3 通过
ConversionUploadLogToolLiveTest       1 通过（真实 MySQL）
AiClientTest                        10 通过（模拟 HTTP）
ConversionAnalysisLiveTest           1 通过（真实 MySQL + 一次模型 API）
合计                                19 通过，0 失败，0 跳过
```

实际查询到 100 条、成功 80、失败 10、初始化 5、处理中 5、未知状态 0。
实际模型回复中包含：

> 该时段创建的回传记录中，成功率为 **80%**。不能仅凭这份数据判断是否发生波动或系统整体是否正常。

人工核对该次回复：引用的数字、未完成状态与统计口径正确，说明了缺少基线和阈值，
没有编造具体故障原因。这个结果只代表本次样例，不是对所有未来模型回复质量的保证。
测试没有新增或删除教学数据。

## 读完代码后自己验证

1. 查看 15:00～16:00 的 summary：100 条、成功 80、失败 10、初始化 5、处理中 5。
2. 检查模型有没有正确说出 80%，有没有区分失败与未完成，有没有说明缺少基线。
3. 可把结束时间改为 15:00，再运行一次，查看另一窗口的分析。这会产生额外一次 API 调用。

两次独立调用之间没有自动共享上下文。模型只分析当次给出的窗口；它不会因为程序
之前跑过另一个窗口，就自动知道 98% 和 80% 之间的变化。

## 本课与下一课的边界

当前是固定的 AI 分析流程：程序决定查什么，然后让模型解释。
下一课会给模型声明工具名称、说明和参数结构，让模型提出调用请求，
再由程序执行、回传结果。那时工具选择才进入模型参与的循环。
