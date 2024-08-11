package com.test.repository.mysql.no_impl_proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/5/9 17:55
 */
public class NoImplProxy implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return new ActualImpl().real();
    }

}
