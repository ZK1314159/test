package com.test.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.test.config.YmlPropertyBeanConfig;
import com.test.service.TestService;
import lombok.extern.slf4j.Slf4j;

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

    @Value("${test.value}")
    private String value;

    @Value("${config.property.id}")
    private Integer id;

    @Value("${test.name}")
    private String name;

    @Override
    public void ymlPropertyTest() {
        log.info("info: " + user.getId() + " " + user.getName());

    }

    @Override
    public void nacosTest() {
        log.info("nacos property: " + value);
    }
}
