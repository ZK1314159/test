package com.test.remote.feign;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.test.entity.PostRequestDto;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/1/16 10:11
 */
@FeignClient(name = "feignTestClient",
             url = "http://localhost:9001",
             fallbackFactory = TestFallbackFactory.class,
             configuration = TestFeignClient.Config.class)
public interface TestFeignClient {

    @GetMapping("/test")
    String test(@RequestParam Integer id);

    @PostMapping(value = "/post", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    PostRequestDto post(PostRequestDto postRequestDto);

    class Config {
        @Bean
        public Encoder formEncoder() {
            return new SpringFormEncoder();
        }
    }

}
