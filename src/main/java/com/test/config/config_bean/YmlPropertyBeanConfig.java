package com.test.config.config_bean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Description
 * @author zengkai
 * Date: 2020/10/7 17:33
 */
@Configuration
@ConfigurationProperties("config.property")
@Getter
@Setter
public class YmlPropertyBeanConfig {

    private Integer id;

    private String name;

}
