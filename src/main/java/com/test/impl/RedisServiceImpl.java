package com.test.impl;

import com.test.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        log.info("set value success");
    }

    @Override
    public void get(String key) {
        String name = (String) redisTemplate.opsForValue().get(key);
        log.info("name: " + name);
    }

}
