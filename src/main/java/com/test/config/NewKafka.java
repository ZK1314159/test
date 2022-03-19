package com.test.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/3/18 22:42
 */
@Configuration
@ConfigurationProperties(prefix = "mykafka")
@Data
public class NewKafka {
    KafkaProperties postscan;
}
