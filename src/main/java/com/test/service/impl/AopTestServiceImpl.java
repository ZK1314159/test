package com.test.service.impl;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.test.service.interfaces.AopTestService;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/5/7 10:02
 */
@Slf4j
@Service
public class AopTestServiceImpl implements AopTestService {

    @Override
    public void test() {
        log.info("AopTestServiceImpl execute!!!");
    }

}
