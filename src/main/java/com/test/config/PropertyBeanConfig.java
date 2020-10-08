package com.test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author zengkai
 * @Date 2020/10/6 15:06
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties("property.test")
public class PropertyBeanConfig {

    private Integer id;

    private String name;

}
