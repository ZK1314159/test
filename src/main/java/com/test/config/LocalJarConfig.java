package com.test.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/7/10 15:25 <br>
 */
@Configuration
@ImportResource("classpath:localJar/localJar.xml")
public class LocalJarConfig {
}
