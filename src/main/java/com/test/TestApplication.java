package com.test;

import com.test.controller.TestController;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@SpringBootApplication
@Slf4j
@EnableFeignClients("com.test.remote.feign")
@EnableDubbo
@EnableCaching
//@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
//        value = NewKafka.class))
public class TestApplication implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ac) {
        applicationContext = ac;
    }

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
        TestController testController = applicationContext.getBean("testController", TestController.class);
        log.info("4238ruir" + testController);
//        AnnotationConfigServletWebServerApplicationContext annotationConfigServletWebApplicationContext =
//                (AnnotationConfigServletWebServerApplicationContext) applicationContext;
//        DefaultListableBeanFactory defaultListableBeanFactory =
//                (DefaultListableBeanFactory) annotationConfigServletWebApplicationContext.getBeanFactory();
//        ConcurrentHashMap<String, Object> singletonObjects =
//                (ConcurrentHashMap<String, Object>) defaultListableBeanFactory.getSingletonMutex();
//        Map<String, Object> aimMap = new HashMap<>();
//        for (Map.Entry<String, Object> entry : singletonObjects.entrySet()) {
//            String beanName = entry.getKey();
//            if (beanName.contains("myProcessor")) {
//                aimMap.put(entry.getKey(), entry.getValue());
//            }
//        }
    }

}
