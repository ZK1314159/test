package com.test.controller;

import com.test.entity.ResultDto;
import com.test.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description
 *
 * @author zengkai
 * Date: 2020/10/8 12:33
 */
@RestController
@RequestMapping("/redis")
public class RedisController {

    @Autowired
    private RedisService redisService;

    @GetMapping("/get")
    public ResultDto get(@RequestParam String key) {
        redisService.get(key);
        return new ResultDto();
    }

    @GetMapping("/set")
    public ResultDto set(@RequestParam String key, @RequestParam String value) {
        redisService.set(key, value);
        return new ResultDto();
    }
}
