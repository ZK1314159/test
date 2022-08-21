package com.test.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.myhexin.zixun.config.User;
import com.myhexin.zixun.config.UserService;
import com.test.config.PropertyBeanConfig;
import com.test.service.CommonService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/24 10:08 <br>
 */
@Service
public class CommonServiceImpl implements CommonService {

    @Autowired
    private User user;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PropertyBeanConfig propertyBeanConfig;

    public static final Logger logger = LoggerFactory.getLogger(CommonServiceImpl.class);

    public String greet(String clientName) {
        logger.info("Greet to " + clientName + ".");
        return clientName;
    }

    public void serve() throws Exception {
        throw new Exception("invoke fail");
    }

    public void walk() {
        logger.info("walking");
    }

    public String print() {
        logger.info("info: " + user.getId() + " " + user.getName());
        userService.print();
        AnnotationConfigServletWebServerApplicationContext annotationConfigServletWebApplicationContext =
                (AnnotationConfigServletWebServerApplicationContext) applicationContext;
        ConfigurableListableBeanFactory defaultListableBeanFactory =
                annotationConfigServletWebApplicationContext.getBeanFactory();
        Boolean contains =
                defaultListableBeanFactory.containsBeanDefinition("com.myhexin.zixun.config.UserAutoConfiguration");
        ConcurrentHashMap<String, Object> singletonObjects =
                (ConcurrentHashMap<String, Object>) defaultListableBeanFactory.getSingletonMutex();
        Map<String, Object> aimMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : singletonObjects.entrySet()) {
            String beanName = entry.getKey();
            if (beanName.contains("UserService") || beanName.contains("UserAutoConfiguration")
            || beanName.contains("userService") || beanName.contains("userAutoConfiguration")
            || beanName.contains("getBean")) {
                aimMap.put(entry.getKey(), entry.getValue());
            }
        }

        Object object = applicationContext.getBean("getBean");
        Object object2 = applicationContext.getBean("userServiceImpl");
        return user.getName();
    }

    public void testProperty() {
        logger.info("info: " + propertyBeanConfig.getId() + " " + propertyBeanConfig.getName());
    }
}
