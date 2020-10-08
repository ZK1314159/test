package com.test.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/16 15:15 <br>
 */
@Configuration
@PropertySource("classpath:configTest/test.properties")
public class PropertiesConfig {
}
