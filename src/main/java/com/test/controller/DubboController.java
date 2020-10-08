package com.test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.test.service.DubboService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/7/9 10:48 <br>
 */
@RestController
@RequestMapping("/dubbo")
public class DubboController {

    @Autowired
    private DubboService dubboService;

    @RequestMapping(value = "/connect", method = RequestMethod.GET)
    String dubboTest(@RequestParam String value) {
        String result = dubboService.invokeProvider(value);
        return result;
    }
}
