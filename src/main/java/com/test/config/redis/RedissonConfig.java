//package com.test.config;
//
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.config.Config;
//import org.redisson.config.SingleServerConfig;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * Description
// *
// * @author zengkai
// * Date: 2021/12/22 11:37
// */
//@Configuration
//public class RedissonConfig {
//
//    @Bean
//    RedissonClient redissonClient() {
//        Config config = new Config();
//        SingleServerConfig singleServerConfig = config.useSingleServer();
//        singleServerConfig.setPassword("123456");
//        singleServerConfig.setAddress("redis://127.0.0.1:6379");
//        config.setLockWatchdogTimeout(60000);
//        return Redisson.create(config);
//    }
//}
