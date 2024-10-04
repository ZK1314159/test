package com.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@Slf4j
@EnableFeignClients("com.test.remote.feign")
@EnableDubbo
@EnableCaching
@EnableAspectJAutoProxy
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
        testSingleBeanPool();
    }

    @SuppressWarnings("unchecked")
    private static void testSingleBeanPool() {
        AnnotationConfigServletWebServerApplicationContext annotationConfigServletWebApplicationContext =
                (AnnotationConfigServletWebServerApplicationContext) applicationContext;
        DefaultListableBeanFactory defaultListableBeanFactory =
                (DefaultListableBeanFactory) annotationConfigServletWebApplicationContext.getBeanFactory();
        ConcurrentHashMap<String, Object> singletonObjects =
                (ConcurrentHashMap<String, Object>) defaultListableBeanFactory.getSingletonMutex();
        Map<String, Object> aimMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : singletonObjects.entrySet()) {
            String beanName = entry.getKey();
            if (beanName.contains("beanTestService")) {
                aimMap.put(entry.getKey(), entry.getValue());
            }
        }
        log.info("aimMap keyset: " + aimMap.keySet());
    }

}
