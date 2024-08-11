package com.test.repository.mysql;

import com.test.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface CourseMapper {

    @Select("select cno as number, cname as courseName from course where cno = #{id}")
    Course getCourse(Integer number);

    @Update("update course set cname = #{courseName} where cno = #{number}")
    void updateCourse(Integer number, String courseName);
}
