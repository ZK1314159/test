# JDK 17 升级说明

项目编译目标、Docker 运行时统一为 Java 17。POM 使用 `maven.compiler.release=17`，产物字节码版本为 61。本机 IDEA 项目 SDK 已切换到已登记的 `temurin-17`；`.idea` 属于本机配置，不纳入 Git。

## 依赖与兼容处理

| 组件 | 升级后版本/处理 |
| --- | --- |
| Spring Boot | 2.7.18，保留 Spring 5 / `javax` API |
| Spring Cloud | 2021.0.9 BOM；统一管理 OpenFeign 等依赖 |
| Lombok | Boot 管理的 1.18.30，修复旧版在 JDK 17 下的编译器反射访问错误 |
| Dubbo | 官方 `dubbo-spring-boot-starter` 3.2.18，替换旧 Alibaba Cloud starter；补充应用名称并关闭 QoS 端口 |
| MyBatis Plus | 3.5.7 |
| Dynamic Datasource | 4.3.1，继续使用适用于 Boot 2 的 starter |
| Redisson | 3.23.5，排除 Spring Data 3.1 适配器，显式使用 `redisson-spring-data-27` |
| Redisson 序列化 | 显式保留旧版默认 `MarshallingCodec`；补充 JBoss Marshalling River 2.1.1.Final |
| MySQL Connector | 新坐标 `com.mysql:mysql-connector-j`，由 Boot 管理为 8.0.33 |
| Fastjson | 1.2.83，保留已有 API |
| Commons Lang / Caffeine | 交由 Boot 管理为 3.12.0 / 2.9.3 |

Feign 的 `FallbackFactory` 改为 Spring Cloud OpenFeign 接口，Hystrix 配置迁移到 Resilience4j。保留 3 秒超时以及线程隔离的核心线程 10、最大线程 50、队列容量 100；两种断路器的统计和拒绝策略并非完全等价。新增显式 bulkhead 依赖，测试验证这些配置实际绑定。降级日志保留原始异常堆栈。

Feign 默认地址仍为 `http://localhost:9001`，可通过 `test.feign.url` 覆盖。

选用 Boot 2.7 是为了将本次范围限制为 JDK 兼容迁移。Boot 2.7 / Cloud 2021 已结束开源维护；本次未迁移到 Boot 3，也未开展全量漏洞治理。参考：[Boot 2.7.18 系统要求](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/getting-started.html)、[Cloud 2021.0.9 版本配套](https://spring.io/blog/2023/12/20/spring-cloud-2021-0-9-aka-jubilee-is-now-available/)、[Dubbo 3.2 的 JDK 17 支持](https://dubbo.incubator.apache.org/en/blog/2023/04/15/advancing-cloud-native-dubbo-3.2-official-release/)。

## 本机验证

使用 IDEA 内置 Maven 和 Temurin 17.0.17，不修改系统全局 Java：

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -s /Users/kaiceng/.m2/settings.xml -B -ntp \
  '-Dtest=AiClientTest,AiControllerTest,ConversionAnalysisServiceTest,ConversionAgentTest,ConversionUploadLogToolTest,BeanUtilTest,*CompatibilityTest' \
  package
```

2026-11-07 验证结果：36 项测试通过，失败 0，错误 0，跳过 0；打包成功，产物为 `target/test-1.0.jar`。

新增兼容测试覆盖：

- 实际应用组件扫描与自动配置启动；仅替换 DataSource、HBaseService、RedissonClient 外部连接。
- Feign 使用本机临时 HTTP 服务正常响应和 500 错误降级。
- Dubbo Javassist 代理生成及调用，无额外 `--add-opens`。
- Redisson 实际 YAML 解析、MarshallingCodec 集合序列化往返。
- MyBatis 加载项目 Mapper、HBase 配置及 HTML Sanitizer 初始化。
- Resilience4j 超时、线程池容量配置绑定。

没有执行依赖真实 MySQL、Redis、HBase 等外部服务的既有集成测试，也未执行需要显式开启的 AI / 回传真实调用测试。上述应用启动测试使用外部连接替身，不代表真实服务联调已通过；未读取历史 Redis 缓存验证跨版本反序列化。Docker daemon 当前不可连接，未构建或运行镜像。
