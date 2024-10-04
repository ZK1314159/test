package com.test.service.direct;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Data
public class BeanTestService {

  private Integer id = 100;

  @Autowired
  private TestRetryService testRetryService;

  public void test() {
    System.out.println("BeanTestService test");
    testRetryService.test(id);
  }

//  public BeanTestService(TestRetryService testRetryService) {
//    this.testRetryService = testRetryService;
//  }

}
