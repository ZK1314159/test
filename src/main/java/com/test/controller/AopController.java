package com.test.controller;

import com.test.entity.ResultDto;
import com.test.service.direct.AopDirectTestService;
import com.test.service.interfaces.CommonService;
import com.test.service.interfaces.IntroductionInterceptorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
  @Autowired
  private AopDirectTestService aopDirectTestService;

  @GetMapping( "/logAfter")
  public ResultDto logAfter() {
    aopDirectTestService.test();
    return new ResultDto();
  }

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
