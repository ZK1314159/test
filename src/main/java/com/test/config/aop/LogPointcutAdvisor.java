package com.test.config.aop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.reflect.Method;

@Slf4j
public class LogPointcutAdvisor extends StaticMethodMatcherPointcutAdvisor {

  public LogPointcutAdvisor(AfterReturningAdvice advice) {
    super(advice);
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    log.info("targetClass: " + targetClass.getName() + ", method: " + method.getName());
    if(!targetClass.getName().equals("com.test.service.direct.AopDirectTestService")) {
      return false;
    }
    // 这里可以添加逻辑来匹配你想要的方法，例如：
    return "test".equals(method.getName());
  }

}

