package com.test.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.test.config.YmlPropertyBeanConfig;
import com.test.service.TestService;

/**
 * @Description
 * @author zengkai
 * @date 2020/10/7 17:35
 */
@Slf4j
@Service
public class TestServiceImpl implements TestService {

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

    @Override
    public void ymlPropertyTest() {
        log.info("info: " + user.getId() + " " + user.getName());
        TestService testService = applicationContext.getBean("testServiceImpl", TestService.class);
        log.info("5734tuiefer8" + testService);
    }

    @Override
    public void nacosTest() {
//        log.info("nacos property: " + value);
    }
}
