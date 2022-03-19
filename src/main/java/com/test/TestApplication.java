package com.test;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(exclude = KafkaAutoConfiguration.class)
@Slf4j
@EnableFeignClients("com.test.feign")
@EnableDubbo
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
//        TestService testService = applicationContext.getBean("testServiceImpl", TestService.class);
//        log.info("4238ruir" + testService);
    }

}
