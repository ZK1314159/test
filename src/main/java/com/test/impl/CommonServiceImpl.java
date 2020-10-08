package com.test.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
        return user.getName();
    }

    public void testProperty() {
        logger.info("info: " + propertyBeanConfig.getId() + " " + propertyBeanConfig.getName());
    }
}
