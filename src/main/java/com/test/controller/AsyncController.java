package com.test.controller;

import com.test.service.direct.AsyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/async")
@RestController
public class AsyncController {

  @Autowired
  private AsyncService asyncService;

  @GetMapping("/request")
  public void asyncEndpoint(String dataId, Boolean returnImmediately, Integer timeout) {
    // 处理异步请求
    asyncService.handleLongPolling(dataId, returnImmediately, timeout);
  }

  @GetMapping("/complete")
  public String completeAsync(String dataId, String newData) {
    // 完成异步请求
    asyncService.onDataChanged(dataId, newData);
    return "Data updated successfully";
  }

}
