package com.test.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.test.service.interfaces.TestService;

/**
 * Description
 *
 * @author zengkai
 * Date: 2020/11/22 23:36
 */
@SpringBootTest
class TestServiceImplTest {

    @Autowired
    private TestService testService;

    @Test
    void testNacosTest() {
        testService.nacosTest();
    }

}