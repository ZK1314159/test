package com.test.config.bean_post_processor;


import java.lang.reflect.Method;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import lombok.extern.slf4j.Slf4j;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/5/7 9:23
 */
@Slf4j
public class MyProxy implements MethodInterceptor {
    private Enhancer enhancer = new Enhancer();

    public Object getProxy(Class clazz) {
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(this);
        return enhancer.create();
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects,
                            MethodProxy methodProxy) throws Throwable {
        log.info("before method !");
        Object result = methodProxy.invokeSuper(o, objects);
        log.info("after method !");
        return result;
    }

}
