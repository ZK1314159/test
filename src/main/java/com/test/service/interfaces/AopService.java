package com.test.service.interfaces;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/24 10:29 <br>
 */
public interface AopService {

    void sayHello(String clientName);

    void sayGoodbye(String clientName);

    void handleException(Exception e);

    void handleAround(ProceedingJoinPoint point) throws Exception;
}
