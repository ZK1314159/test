package com.test.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.test.entity.ResultDto;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/7/10 22:42
 */
@RestController
@RequestMapping("/")
public class NginxTestController {

    @GetMapping("/")
    public ResultDto testNginx() {
        return new ResultDto();
    }

}
