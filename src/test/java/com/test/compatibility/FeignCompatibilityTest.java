package com.test.compatibility;

import com.sun.net.httpserver.HttpServer;
import com.test.remote.feign.TestFallbackFactory;
import com.test.remote.feign.TestFeignClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JAutoConfiguration;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FeignCompatibilityTest {

    @Test
    void callsEndpointAndUsesFallbackOnServerFailure() throws Exception {
        AtomicInteger status = new AtomicInteger(200);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/test", exchange -> {
            requests.incrementAndGet();
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            try (java.io.OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class,
                            HttpClientConfiguration.class, FeignAutoConfiguration.class,
                            Resilience4JAutoConfiguration.class))
                    .withBean(CircuitBreakerRegistry.class, CircuitBreakerRegistry::ofDefaults)
                    .withBean(TimeLimiterRegistry.class, TimeLimiterRegistry::ofDefaults)
                    .withUserConfiguration(FeignConfig.class)
                    .withPropertyValues("feign.circuitbreaker.enabled=true",
                            "spring.cloud.circuitbreaker.bulkhead.resilience4j.enabled=false",
                            "test.feign.url=http://127.0.0.1:" + server.getAddress().getPort())
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        TestFeignClient client = context.getBean(TestFeignClient.class);
                        assertThat(client.test(1)).isEqualTo("ok");
                        status.set(500);
                        assertThat(client.test(2)).isNull();
                        assertThat(requests.get()).isEqualTo(2);
                    });
        } finally {
            server.stop(0);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableFeignClients(clients = TestFeignClient.class)
    @Import(TestFallbackFactory.class)
    static class FeignConfig {
    }
}
