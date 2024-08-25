package com.test.config.aop;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
  public AfterLogAdvice afterLogAdvice() {
    return new AfterLogAdvice();
  }

  @Bean
  public LogPointcutAdvisor logPointcutAdvisor(AfterLogAdvice advice) {
    return new LogPointcutAdvisor(advice);
  }

}
