package com.test.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.test.service.RedisService;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/5/1 10:48
 */
@SpringBootTest
class RedisServiceImplTest {

    @Autowired
    private RedisService redisService;

    @Test
    void test() {
        String key = "test";
        String value = "美女";
        redisService.set(key, value);
        String result = redisService.get(key);
    }

    @Test
    void testStringRedisTemplate() {
        String key = "string";
        String value = "美女";
        redisService.stringSet(key, value);
        String result = redisService.stringGet(key);
    }

}