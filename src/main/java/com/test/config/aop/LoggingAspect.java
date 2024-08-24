package com.test.config.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

  @After("execution(* com.test.service.impl.RedisServiceImpl.get(..))")
  public void logAfter() {
    log.info("Your method has been executed.");
  }

}
