package com.test.compatibility;

import com.test.TestApplication;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import com.test.remote.feign.TestFeignClient;
import com.test.service.direct.HBaseService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

// 仅替换外部连接，验证实际组件扫描、自动配置和代理在 JDK 17 下能启动。
@SpringBootTest(classes = TestApplication.class, properties = {
        "logging.level.com.test.config.aop=WARN", "spring.main.banner-mode=off"
})
class ApplicationCompatibilityTest {

    @MockBean(name = "getHbaseService")
    private HBaseService hbaseService;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean(name = "dataSource")
    private DataSource dataSource;

    @Autowired
    private ApplicationContext context;

    @Test
    void startsApplicationWithExternalConnectionsReplaced() {
        assertThat(context.getBean(TestFeignClient.class)).isNotNull();
        assertThat(context.getBean("sqlSessionFactory")).isNotNull();
        io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig bulkhead =
                context.getBean(ThreadPoolBulkheadRegistry.class).getDefaultConfig();
        assertThat(bulkhead.getCoreThreadPoolSize()).isEqualTo(10);
        assertThat(bulkhead.getMaxThreadPoolSize()).isEqualTo(50);
        assertThat(bulkhead.getQueueCapacity()).isEqualTo(100);
        assertThat(context.getBean(TimeLimiterRegistry.class).getDefaultConfig().getTimeoutDuration())
                .isEqualTo(Duration.ofSeconds(3));
        assertThat(Runtime.version().feature()).isGreaterThanOrEqualTo(17);
    }
}
