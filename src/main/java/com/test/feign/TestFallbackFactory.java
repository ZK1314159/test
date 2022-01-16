package com.test.feign;

import feign.hystrix.FallbackFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;
import com.test.entity.PostRequestDto;

@Component
@Slf4j
public class TestFallbackFactory implements FallbackFactory<TestFeignClient> {

    @Override
    public TestFeignClient create(Throwable cause) {
        return new TestFeignClient() {
            @Override
            public String test(@RequestParam Integer id) {
                String msg = "[测试服务: TestFeignClient.test] 调用异常" + ", 异常原因: " + cause
                        + ", 入参为=> id: " + id ;
                log.error(msg);
                return null;
            }

            @Override
            public PostRequestDto post(PostRequestDto postRequestDto) {
                String msg = "[测试服务: TestFeignClient.post] 调用异常" + ", 异常原因: " + cause
                        + ", 入参为=> postRequestDto: " + postRequestDto ;
                log.error(msg);
                return null;
            }
        };
    }

}
