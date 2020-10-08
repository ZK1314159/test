package com.test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.test.entity.ResultDto;
import com.test.service.CommonService;
import com.test.service.IntroductionInterceptorService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/27 17:18 <br>
 */
@RestController
@RequestMapping("/aop")
public class AopController {

    @Autowired
    private CommonService commonService;

    @RequestMapping(value = "/beforeAndAfter", produces = "application/json", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResultDto aopTest(@RequestParam String clientName) {
        commonService.greet(clientName);
        ResultDto result = new ResultDto();
        return result;
    }

    @RequestMapping(value = "/exception", produces = "application/json", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResultDto aopExceptionTest() throws Exception {
        commonService.serve();
        ResultDto result = new ResultDto();
        return result;
    }

    @RequestMapping(value = "/around", produces = "application/json", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResultDto aopAroundTest() {
        commonService.walk();
        ResultDto result = new ResultDto();
        return result;
    }

    @RequestMapping(value = "/introduction", produces = "application/json", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public ResultDto aopIntroductionTest() {
        IntroductionInterceptorService introduction = (IntroductionInterceptorService) commonService;
        introduction.sing();
        ResultDto result = new ResultDto();
        return result;
    }
}
