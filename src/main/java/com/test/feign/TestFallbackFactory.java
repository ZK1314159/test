package com.test.feign;

import feign.hystrix.FallbackFactory;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TestFallbackFactory implements FallbackFactory<TestFeignClient> {

    @Override
    public TestFeignClient create(Throwable cause) {
        return (id) -> {
            String msg = "[测试服务: TestFeignClient.test] 调用异常" + ", 异常原因: " + cause
                    + ", 入参为=> id: " + id ;
            log.error(msg);
            return null;
        };
    }

}
