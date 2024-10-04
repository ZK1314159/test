package com.test.service.impl;

import com.test.config.config_bean.YmlPropertyBeanConfig;
import com.test.service.interfaces.TestService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zengkai
 * @Description
 * @date 2020/10/7 17:35
 */
@Slf4j
@Service
@EnableRetry
public class TestServiceImpl2 implements TestService {

  @Autowired
  private YmlPropertyBeanConfig user;

  @Autowired
  private ApplicationContext applicationContext;

//    @Value("${test.value}")
//    private String value;

  @Value("${config.property.id}")
  private Integer id;

  @Value("${test.name}")
  private String name;

  public static void main(String[] args) throws Exception {
    MDC.put("UUID", "TEST");
    String word = MDC.get("UUID");
    Thread thread = new Thread(() -> {
      MDC.put("UUID", word);
      System.out.println(MDC.get("UUID"));
    });
    thread.start();
  }

  private void test() {
    // 数组转集合
    // int[]无法直接转换为List<Integer>, 需要自己手动写循环
    // Integer[]可以直接转换为List<Integer>
    Integer[] ints = new Integer[]{1, 3, 5};
    List<Integer> tmpList = Arrays.asList(ints);
    List<Integer> list = new ArrayList<>(tmpList);

    // 集合转数组
    Integer[] tmpArray = new Integer[list.size()];
    Integer[] array = list.toArray(tmpArray);
  }

  @Override
  public void ymlPropertyTest() {
    log.info("info: " + user.getId() + " " + user.getName());
    TestService testService = applicationContext.getBean("testServiceImpl", TestService.class);
    log.info("5734tuiefer8" + testService);
  }

  @Override
  public void nacosTest() {
//        log.info("nacos property: " + value);

    for (int i = 0; i < 2; i++) {
      try {
//                throw new NullPointerException();
      } catch (Exception e) {
        continue;
      }
      break;
    }

  }

  @Retryable
  public void retryTest(Integer count) {
    if (count == 3) {
      log.info("bad key");
      throw new RuntimeException("bad count");
    }
  }

}
