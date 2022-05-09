package com.test.config.bean_post_processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/5/5 17:06
 */
@Component
public class AopProcessor implements BeanPostProcessor {

    @Nullable
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        Object proxy = null;
        if ("aopTestServiceImpl".equals(beanName)) {
            MyProxy myProxy = new MyProxy();
            proxy = myProxy.getProxy(bean.getClass());
        }
        return proxy;
    }

}
