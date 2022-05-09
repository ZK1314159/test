package com.test.config.bean_post_processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.test.controller.TestController;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/5/5 17:06
 */
@Component("myProcessor")
public class MyProcessor implements BeanPostProcessor {

    @Nullable
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if ("testController".equals(beanName)) {
            TestController testController = (TestController) bean;
            testController.setId(7);
        }
        return bean;
    }

}
