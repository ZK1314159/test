package com.test.service.interfaces;

import com.test.entity.Course;

/**
 * Description
 *
 * @author zengkai
 * Date: 2020/10/8 12:28
 */
public interface RedisService {

    void set(String key, Object value);

    String get(String key);

    Object getObject(String key);

    void stringSet(String key, String value);

    String stringGet(String key);

    void setCourse(String key, Course value);

    Course getCourse(String key);

}
