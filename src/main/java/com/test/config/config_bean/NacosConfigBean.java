package com.test.config.config_bean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/6/14 21:15
 */
@RefreshScope
@Configuration
@ConfigurationProperties("test")
@Data
public class NacosConfigBean {

    private String newdata;

}
