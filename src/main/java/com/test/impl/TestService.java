package com.test.impl;

import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@EnableRetry
public class TestService {

  @Retryable
  public void test(Integer count) {
    if (count == 3) {
      System.out.println("bad key");
      throw new RuntimeException("bad count");
    }
  }


}
