package com.test.impl;

import com.test.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Description
 *
 * @author zengkai
 * Date: 2020/10/8 12:29
 */
@Slf4j
@Service
public class RedisServiceImpl implements RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        log.info("set value success");
    }

    @Override
    public String get(String key) {
        String result = (String) redisTemplate.opsForValue().get(key);
        log.info("name: " + result);
        return result;
    }

    @Override
    public void stringSet(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    @Override
    public String stringGet(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

}
