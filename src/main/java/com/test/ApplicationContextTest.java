package com.test;

import com.test.service.direct.BeanTestService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ApplicationContextTest {

  public static void main(String[] args) {
    BeanTestService beanTestService;
    beanTestService = (BeanTestService) startByXml();
//    beanTestService = (BeanTestService) startByAnnotation();
    beanTestService.test();
  }

  public static Object startByXml() {
    System.out.println("ApplicationContextTest startByXml");
    ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("beanConfig.xml");
    return applicationContext.getBean("beanTestService");
  }

  public static Object startByAnnotation() {
    System.out.println("ApplicationContextTest startByAnnotation");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(BeanTestService.class);
    context.refresh();
    return context.getBean("beanTestService");
  }

}
