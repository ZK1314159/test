package com.test.service.direct;

import org.springframework.stereotype.Component;

@Component
//@EnableRetry
public class TestRetryService {

//  @Retryable
  public void test(Integer count) {
    if (count == 3) {
      System.out.println("bad key");
      throw new RuntimeException("bad count");
    } else {
      System.out.println("good key");
    }
  }

}
