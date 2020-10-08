package com.test.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.test.service.CommonService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Description
 * @Author zengkai
 * @Date 2020/10/6 11:00
 */
@SpringBootTest
class CommonServiceImplTest{

    @Autowired
    private CommonService commonService;

    @Test
    void test() {
        String name = commonService.print();
        Assertions.assertEquals("zengkai", name);
    }
}