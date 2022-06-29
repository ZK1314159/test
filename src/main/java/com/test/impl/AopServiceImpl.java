package com.test.impl;

import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareParents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.test.service.AopService;
import com.test.service.IntroductionInterceptorService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/24 10:30 <br>
 */
@Aspect
@Service
public class AopServiceImpl implements AopService {

    public static final Logger logger = LoggerFactory.getLogger(AopServiceImpl.class);

    @Before("execution(* *..*.CommonService.greet(..)) && args(clientName)")
    public void sayHello(String clientName) {
        logger.info("hello! " + clientName);
    }

    @AfterReturning(value = "execution(* *..*.CommonService.greet(..))", returning = "clientName")
    public void sayGoodbye(String clientName) {
        logger.info("Goodbye! " + clientName);
    }

    @AfterThrowing(value = "execution(* com.test.service.CommonService.serve(..))", throwing = "e")
    public void handleException(Exception e) {
        logger.info("Catch a exception! reason : " + e.getMessage());
    }

    @Around("execution(* *..*.CommonService.walk(..))")
    public void handleAround(ProceedingJoinPoint point) throws Exception {
        logger.info("start walk");
        try {
            point.proceed();
        }
        catch (Throwable e) {
            throw new Exception(e.getMessage(), e);
        }
        logger.info("end walk");
    }

    @DeclareParents(value = "com.test.impl.CommonServiceImpl",
                    defaultImpl = com.test.impl.IntroductionInterceptorServiceImpl.class)
    public IntroductionInterceptorService introductionInterceptorService;

    public static void main(String[] args) {
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            Thread.sleep(1000);
        } catch (Exception e) {

        }
        System.out.println(stopWatch.getTime());
        System.out.println(stopWatch.getNanoTime());
    }
}
