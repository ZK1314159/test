package com.test.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/11 22:32
 */
@SpringBootTest
class PropertyBeanConfigTest {

    @Autowired
    private PropertyBeanConfig propertyBeanConfig;

    @Test
    void test() {
        Integer id = propertyBeanConfig.getId();
    }

}