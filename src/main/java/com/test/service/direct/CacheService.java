package com.test.service.direct;

import com.test.entity.Course;
import com.test.repository.mysql.CourseMapper;
import lombok.Data;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

//@Component
@Data
public class CacheService {

  private CourseMapper courseMapper;

  public CacheService(CourseMapper courseMapper) {
    this.courseMapper = courseMapper;
  }

  @Cacheable(value = "course", keyGenerator = "myKeyGenerator")
  public Course getCourse(Integer number) {
    System.out.println("get cache");
    return courseMapper.getCourse(number);
  }

  @CacheEvict(value = "course", keyGenerator = "myKeyGenerator")
  public void updateCourse(Integer number, String courseName) {
    System.out.println("update cache");
    courseMapper.updateCourse(number, courseName);
  }

}
