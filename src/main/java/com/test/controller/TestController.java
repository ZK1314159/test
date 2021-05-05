package com.test.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.test.entity.ResultDto;
import com.test.entity.User;
import com.test.service.CommonService;
import com.test.service.TestService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/16 14:32 <br>
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private CommonService commonService;

    @Autowired
    private TestService testService;

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Value("${test.name}")
    private String name;

    @Value("${spring.datasource.dynamic.datasource.master.username}")
    private String username;

    @Value("${spring.datasource.dynamic.datasource.master.password}")
    private String password;

    @RequestMapping(value = "/log", produces = "application/json", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResultDto logTest() {
        logger.info("test log!");
        ResultDto result = new ResultDto();
        return result;
    }

    @RequestMapping(value = "/properties", produces = "application/json", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResultDto propertiesTest() {
        logger.info("test.name={}", name);
        logger.info("username={}", username);
        logger.info("password={}", password);
        ResultDto result = new ResultDto();
        return result;
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public void getTest(@RequestParam String userId) {
        logger.info("get");
    }

    @RequestMapping(value = "/post", method = RequestMethod.POST)
    public void postTest(@RequestBody User user) {
        logger.info("post");
        logger.info("user.userId : " + user.getUserId());
    }

    @GetMapping("/starter")
    public void staterTest() {
        commonService.print();
    }

    @GetMapping("/property")
    public ResultDto propertyTest() {
        commonService.testProperty();
        return new ResultDto();
    }

    @GetMapping("/yml")
    public ResultDto ymlPropertyTest() {
        testService.ymlPropertyTest();
        return new ResultDto();
    }
}
