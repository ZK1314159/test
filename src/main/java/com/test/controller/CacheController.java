package com.test.controller;

import com.alibaba.fastjson.JSON;
import com.test.entity.Course;
import com.test.entity.ResultDto;
import com.test.service.direct.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/cache")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @GetMapping("/getCourse")
    public ResultDto getCourse(@RequestParam("number") Integer number) {
        Course course = cacheService.getCourse(number);
        return new ResultDto(JSON.toJSONString(course));
    }

    @GetMapping("/updateCourse")
    public ResultDto updateCourse(@RequestParam("number") Integer number, @RequestParam("courseName") String courseName) {
        cacheService.updateCourse(number, courseName);
        return new ResultDto();
    }

}