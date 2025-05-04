package com.test.service.direct;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AsyncService {

  // 按 DataID 分组的挂起请求
  ConcurrentHashMap<String, List<AsyncContext>> pendingRequests = new ConcurrentHashMap<>();

  // 示例：处理客户端的长轮询请求
  public void handleLongPolling(String dataId, Boolean returnImmediately, Integer timeout) {
    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    assert servletRequestAttributes != null;
    HttpServletRequest request = servletRequestAttributes.getRequest();
    // 1. 创建异步上下文
    AsyncContext asyncContext = request.startAsync();
    asyncContext.setTimeout(timeout * 1000L); // 设置超时时间
    asyncContext.addListener(new AsyncListener() {
      @Override
      public void onComplete(AsyncEvent event) throws IOException {
        // 处理完成事件
        System.out.println("Async request completed");
      }

      @Override
      public void onTimeout(AsyncEvent event) throws IOException {
        // 处理超时事件
        ServletResponse asyncResponse = asyncContext.getResponse();
        asyncResponse.setContentType("text/plain");
        PrintWriter out = asyncResponse.getWriter();
        out.print("Async request timed out");
        asyncContext.complete();
      }

      @Override
      public void onError(AsyncEvent event) throws IOException {
        // 处理错误事件
        System.out.println("Error occurred in async request");
        asyncContext.complete();
      }

      @Override
      public void onStartAsync(AsyncEvent event) throws IOException {
        // 处理开始异步事件
        System.out.println("Async request started");
      }
    });

    // 是否立刻返回数据
    if (returnImmediately) {
      String data = "new data";
      writeResponse(asyncContext, data);
    } else {
      // 4. 无变更：挂起请求，注册到 pendingRequests
      pendingRequests.computeIfAbsent(dataId, k -> new CopyOnWriteArrayList<>()).add(asyncContext);
    }
  }

  // 写入数据
  private void writeResponse(AsyncContext asyncContext, String data) {
    try {
      // 获取响应对象并写入数据
      ServletResponse asyncResponse = asyncContext.getResponse();
      asyncResponse.setContentType("text/plain");
      PrintWriter out = asyncResponse.getWriter();
      out.print(data);
      asyncContext.complete();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // 示例：数据变更事件处理
  public void onDataChanged(String dataId, String newData) {
    List<AsyncContext> contexts = pendingRequests.remove(dataId);
    if (contexts != null) {
      for (AsyncContext ctx : contexts) {
        // 返回新数据并完成请求
        writeResponse(ctx, newData);
      }
    }
  }

}
