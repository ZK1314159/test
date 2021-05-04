package com.test.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.test.entity.User;
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

    @Test
    void testRedisTemplateForObject() {
        String key = "jsontest";
        User user = new User();
        user.setUserId(1234);
        user.setUserName("hahah4");
        redisService.set(key, user);
        User result = (User) redisService.getObject(key);
    }

}