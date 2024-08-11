package com.test.repository.mysql.no_impl_proxy;

import java.lang.reflect.Proxy;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/5/9 17:57
 */
public class ProxyClient {

    public void invoke() {
        NoImplProxy proxy = new NoImplProxy();
        NoImplMapper noImplMapper = (NoImplMapper) Proxy.newProxyInstance(NoImplMapper.class.getClassLoader(),
                new Class[]{NoImplMapper.class}, proxy);
        Integer result = noImplMapper.test();
    }

    public static void main(String[] args) {
        new ProxyClient().invoke();
    }
}
