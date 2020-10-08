package com.test.service;

/**
 * Description
 *
 * @author zengkai
 * Date: 2020/10/8 12:28
 */
public interface RedisService {

    void set(String key, Object value);

    void get(String key);
}
