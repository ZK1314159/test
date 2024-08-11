package com.test.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.test.service.interfaces.IntroductionInterceptorService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/27 11:23 <br>
 */
@Service
public class IntroductionInterceptorServiceImpl implements IntroductionInterceptorService {

    public static final Logger logger = LoggerFactory.getLogger(IntroductionInterceptorServiceImpl.class);

    public void sing() {
        logger.info("sing");
    }
}
