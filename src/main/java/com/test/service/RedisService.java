package com.test.service;

/**
 * Description
 *
 * @author zengkai
 * Date: 2020/10/8 12:28
 */
public interface RedisService {

    void set(String key, Object value);

    String get(String key);

    void stringSet(String key, String value);

    String stringGet(String key);

}
