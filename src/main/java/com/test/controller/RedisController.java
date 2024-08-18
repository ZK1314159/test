package com.test.controller;

import com.alibaba.fastjson.JSON;
import com.test.entity.Course;
import com.test.entity.ResultDto;
import com.test.service.interfaces.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Description
 *
 * @author zengkai
 * Date: 2020/10/8 12:33
 */
@RestController
@RequestMapping("/redis")
public class RedisController {

    @Autowired
    private RedisService redisService;

    @GetMapping("/get")
    public ResultDto get(@RequestParam String key) {
        redisService.get(key);
        return new ResultDto();
    }

    @GetMapping("/set")
    public ResultDto set(@RequestParam String key, @RequestParam String value) {
        redisService.set(key, value);
        return new ResultDto();
    }

    @GetMapping("/setCourse")
    public ResultDto setCourse(@RequestParam String key, @RequestParam String value) {
        Course course = JSON.parseObject(value, Course.class);
        redisService.setCourse(key, course);
        return new ResultDto();
    }

    @GetMapping("/getCourse")
    public ResultDto getCourse(@RequestParam String key) {
        Course course = redisService.getCourse(key);
        String result = JSON.toJSONString(course);
        return new ResultDto(result);
    }

}
