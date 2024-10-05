package com.test;

import com.test.service.direct.BeanTestService;
import com.test.service.direct.TestRetryService;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContextTest {

  public static void main(String[] args) {
    BeanTestService beanTestService;
    beanTestService = (BeanTestService) startByAnnotation();
//    beanTestService = (BeanTestService) startByXml();
    beanTestService.test();
  }

  @SuppressWarnings("unchecked")
  public static Object startByAnnotation() {
    System.out.println("ApplicationContextTest startByAnnotation");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    // 即使要注册的类没有@Component注解，也可以注册到容器中
    // 但是如果要使用@Autowired注入，所有相关bean都得列入register方法参数中
    context.register(BeanTestService.class, TestRetryService.class);
    context.refresh();
    DefaultListableBeanFactory defaultListableBeanFactory =
      (DefaultListableBeanFactory) context.getBeanFactory();
    ConcurrentHashMap<String, Object> singletonObjects =
      (ConcurrentHashMap<String, Object>) defaultListableBeanFactory.getSingletonMutex();
    return singletonObjects.get("beanTestService");
  }

  public static Object startByXml() {
    System.out.println("ApplicationContextTest startByXml");
    ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("beanConfig.xml");
    return applicationContext.getBean("beanTestService");
  }

}
