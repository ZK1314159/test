package com.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.extern.slf4j.Slf4j;
import com.alibaba.nacos.spring.context.annotation.config.NacosPropertySource;

//@EnableDubbo
@NacosPropertySource(dataId = "test", autoRefreshed = true)
@SpringBootApplication
@Slf4j
public class TestApplication implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ac) {
        applicationContext = ac;
    }

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
//        TestService testService = applicationContext.getBean("testServiceImpl", TestService.class);
//        log.info("4238ruir" + testService);
    }
}
