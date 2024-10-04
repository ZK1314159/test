package com.test.controller;

import com.test.service.interfaces.TestService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test2")
@Slf4j
@Data
public class TestController2 {

  @Autowired
  @Qualifier("testServiceImpl")
  private TestService testService;
  @Autowired
  private Map<String, TestService> testServiceMap;

  @RequestMapping("/ymlPropertyTest")
  public void ymlPropertyTest() {
//    testService.retryTest(3);
    testServiceMap.get("testServiceImpl").retryTest(3);
  }

}
