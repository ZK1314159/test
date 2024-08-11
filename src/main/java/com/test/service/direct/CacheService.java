package com.test.service.direct;

import com.test.entity.Course;
import com.test.repository.mysql.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class CacheService {

  @Autowired
  private CourseMapper courseMapper;

  @Cacheable(value = "course", key = "#number")
  public Course getCourse(Integer number) {
    System.out.println("get cache");
    return courseMapper.getCourse(number);
  }

  @CacheEvict(value = "course", key = "#number")
  public void updateCourse(Integer number, String courseName) {
    System.out.println("update cache");
    courseMapper.updateCourse(number, courseName);
  }

}
