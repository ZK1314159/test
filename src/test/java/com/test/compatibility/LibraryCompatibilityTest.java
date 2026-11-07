package com.test.compatibility;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.test.entity.User;
import com.test.remote.dubbo.DubboService;
import com.test.repository.mysql.UserMapper;
import io.netty.buffer.ByteBuf;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.javassist.JavassistProxyFactory;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.junit.jupiter.api.Test;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.redisson.codec.MarshallingCodec;
import org.owasp.html.Sanitizers;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LibraryCompatibilityTest {

    @Test
    void dubboGeneratesAndInvokesProxyWithoutOpeningJdkModules() {
        JavassistProxyFactory factory = new JavassistProxyFactory();
        Invoker<DubboService> invoker = factory.getInvoker(word -> "hello " + word,
                DubboService.class, URL.valueOf("dubbo://127.0.0.1:20880/" + DubboService.class.getName()));
        try {
            DubboService proxy = factory.getProxy(invoker);
            assertThat(proxy.invokeProvider("java17")).isEqualTo("hello java17");
        } finally {
            invoker.destroy();
        }
    }

    @Test
    void redissonRetainsMarshallingCodecAndRoundTripsJdkCollection() throws Exception {
        Config config;
        try (InputStream input = getClass().getResourceAsStream("/redisson.yml")) {
            config = Config.fromYAML(input);
        }
        Codec codec = config.getCodec();
        assertThat(codec).isInstanceOf(MarshallingCodec.class);
        ArrayList<String> value = new ArrayList<>(Arrays.asList("java", "17"));
        ByteBuf buffer = codec.getValueEncoder().encode(value);
        try {
            assertThat(codec.getValueDecoder().decode(buffer, null)).isEqualTo(value);
        } finally {
            buffer.release();
        }
    }

    @Test
    void mybatisLoadsProjectMapperWithoutConnectingToDatabase() throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(mock(DataSource.class));
        org.apache.ibatis.session.Configuration configuration = factory.getObject().getConfiguration();
        configuration.addMapper(UserMapper.class);
        assertThat(configuration.hasMapper(UserMapper.class)).isTrue();
        User user = new User();
        user.setUserId(17);
        assertThat(user.getUserId()).isEqualTo(17);
    }

    @Test
    void hbaseAndSanitizerInitializeWithResolvedDependencies() {
        assertThat(HBaseConfiguration.create().get("hbase.client.retries.number")).isNotEmpty();
        assertThat(Sanitizers.FORMATTING.sanitize("<b>hello</b><script>alert(1)</script>"))
                .isEqualTo("<b>hello</b>");
    }
}
