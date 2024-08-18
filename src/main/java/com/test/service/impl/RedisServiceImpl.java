package com.test.service.impl;

import com.test.entity.Course;
import com.test.service.interfaces.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

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

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void set(String key, Object value) {

        redisTemplate.opsForValue().setIfAbsent("fjdjf", "1234", Duration.ofSeconds(300));

        String lockKey = "fsdjfsdk234234";
        RLock lock = redissonClient.getLock(lockKey);
        log.info(Thread.currentThread().getName() + ": " + Thread.currentThread().getId());
        try {
            lock.lock();
//            lock.lock(40, TimeUnit.SECONDS);
//            lock.tryLock();
        } finally {
//            lock.unlock();
        }
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
    public Object getObject(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void stringSet(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    @Override
    public String stringGet(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void setCourse(String key, Course value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public Course getCourse(String key) {
        return (Course) redisTemplate.opsForValue().get(key);
    }

}
