package com.test.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/1/16 10:11
 */
@FeignClient(name = "feignTestClient",
             url = "http://localhost:9001",
             fallbackFactory = TestFallbackFactory.class)
public interface TestFeignClient {

    @GetMapping("/test")
    String test(@RequestParam Integer id);

}
