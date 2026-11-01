# 第一课：先写一个 Agent 能使用的查询工具

本课目标：从本机 MySQL 查询一个小时内创建的回传记录，返回结构化统计。
这一课暂时不调用模型，也不判断异常；先把 Agent 获取事实的能力做出来。

## 这段代码在 Agent 中的位置

完整流程是：用户提出目标 → 模型选择工具及参数 → 程序执行工具 → 将结果交回模型 → 模型继续调查或回答。

本课实现其中的“程序执行工具”。现在由我们直接指定参数；后续再由模型选择参数。

MySQL 调用不要求 MCP。本课直接使用项目已有的 JDBC：

```text
ConversionUploadLogTool.querySummary(start, end)
    → JdbcTemplate
    → 本机 MySQL
    → 统计结果 Map（后续作为 JSON 交给模型）
```

MCP 是一种连接 AI 应用与工具服务的协议。以后需要让不同应用复用这个查询工具时，
可以再添加 MCP 服务层，底层查询逻辑仍然可以复用。
参考：[MCP 官方架构说明](https://modelcontextprotocol.io/docs/learn/architecture)。

## 从哪里读代码

1. `src/main/java/com/test/agent/ConversionUploadLogTool.java`：本课核心，先阅读它。
2. `src/test/java/com/test/agent/ConversionUploadLogToolLiveTest.java`：连接本机 MySQL 并调用工具的运行入口。
3. `src/test/java/com/test/agent/ConversionUploadLogToolTest.java`：验证空数据、非法参数、状态结果等边界。

核心入口只有一个：

```java
Map<String, Object> querySummary(LocalDateTime start, LocalDateTime end)
```

- `start/end`：工具参数，以数据库当地时间解释。
- 参数校验：只允许大于零且不超过一小时的范围，让调用边界清楚。
- 固定 SQL：查询 `tdtracking.tracking_conversion_upload_log`，时间使用绑定参数。
- `Map`：工具输出，不带用户 ID、设备 ID 或原始回传报文。
- `successRate`：成功条数 / 全部条数，取值为 0～1；空窗口返回 `null`。

计数由数据库完成，模型将来根据计数解释现象。工具不接收任意 SQL。
工具本身只有 SELECT；运行入口额外使用只读事务和 10 秒查询超时。
这并不等同于数据库账户已被授予只读权限。

## 先明确统计口径

时间范围为 `[start, end)`：包含开始时间，不包含结束时间，相邻小时不会重复统计。

本课按 `created_at` 归组，观察这些记录在查询时的当前状态：

| upload_status | 含义 | 输出字段 |
| --- | --- | --- |
| 20 | 成功 | success |
| -1 | 失败 | failed |
| 0 | 初始化 | initialized |
| 10 | 处理中 | processing |
| NULL 或其他值 | 未知状态 | unknown |

状态定义参考 tdtracking 项目的 `UploadStatusEnum`，并已核对本地实际表结构。
重试会更新同一条记录，因此这不是某小时实际发送尝试次数，也无法还原过去某一时刻的状态。
初始化与处理中单独列出，不直接当成失败。第一版也不会仅凭成功率就断定异常。

## 教学数据

首次检查时本地表为空，已使用同目录下的 `seed-demo.sql` 插入并保留 200 条记录。
本次标记为 `AGENT_DEMO_c5ff9a10d5d011f1a332a8c6e62e326f`，全部是合成数据。

| 时间范围（Asia/Shanghai） | 总数 | 成功 | 失败 | 初始化 | 处理中 |
| --- | --- | --- | --- | --- | --- |
| 2026-11-01 14:00～15:00 | 100 | 98 | 2 | 0 | 0 |
| 2026-11-01 15:00～16:00 | 100 | 80 | 10 | 5 | 5 |

先使用固定历史时间复现结果，避免学习过程中“最近一小时”不断移动。
省略 `AGENT_WINDOW_END` 时，运行器用数据库的 `NOW()` 查询滚动的最近一小时。
换成新时间窗口后可能没有样例，这表示无数据，不表示系统正常或异常。

`seed-demo.sql` 每次运行都会按当时的整点新增 200 条带独立标记的数据，
不删除原有数据。重复运行会增加对应窗口的统计总数；不要为了重跑测试重复造数据。

可以在数据库客户端只读查看本次样例：

```sql
SELECT message_id, created_at, callback_media, upload_status
FROM tdtracking.tracking_conversion_upload_log
WHERE message_id LIKE 'AGENT_DEMO_c5ff9a10d5d011f1a332a8c6e62e326f%'
ORDER BY created_at, id;
```

## 在 IDEA 运行

打开 `ConversionUploadLogToolLiveTest`，在该测试的运行配置中设置：

```text
AGENT_MYSQL_TEST=true
AGENT_WINDOW_END=2026-11-01T16:00:00
```

直接运行测试，查看控制台的 `AGENT_TOOL_RESULT=`。
不需要启动完整 Spring Boot 应用、Redis，也不需要模型 API Key。
运行入口复用 `application-dev.yml` 中 master 数据源的账号密码，
URL 固定为 `127.0.0.1:3306/tdtracking`。也可通过进程环境变量
`AGENT_MYSQL_USER` / `AGENT_MYSQL_PASSWORD` 覆盖，不要把密码写进代码。

IDEA 的测试 JRE 选择完整的 Corretto JDK 8。终端使用 IDEA 内置 Maven；
本机 `java_home -v 1.8` 可能选中缺少 `tools.jar` 的浏览器插件 JRE，
下面指定已安装的完整 JDK 版本，以兼容项目旧版 Lombok：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8.0_462) \
AGENT_MYSQL_TEST=true \
AGENT_WINDOW_END=2026-11-01T16:00:00 \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -Dtest=ConversionUploadLogToolTest,ConversionUploadLogToolLiveTest test
```

## 后续课程

本课已用 IDEA 内置 Maven 和 Corretto 8 实际验证：两轮各 4 个测试通过，
分别查询上述两个时间窗口，得到成功率 `0.8` 和 `0.98`。
测试后独立回读确认 200 条教学数据仍然保留。没有调用模型 API。

1. 本课：直接运行工具，获得真实数据。
2. 把工具结果提供给模型，让它解释数据；这是固定的分析流程。
3. 为模型声明工具，让模型返回工具调用请求，由程序执行并回传结果。
4. 添加上一小时或渠道工具，让模型根据结果继续查询，并限制调用轮数。

第三、四课会把“模型选择行动 → 工具执行 → 新观察 → 再选择行动”的循环具体写出来。
