package com.test.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Description：RedisTemplate配置
 *
 * @author zeng.kai <br>
 * CreateDate：2020/9/1 23:48 <br>
 */
@Configuration
public class RedisConfig {

    //读取pool配置
    @Bean
    public GenericObjectPoolConfig poolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(10);
        config.setMaxIdle(20);
        config.setMaxTotal(100);
        config.setMaxWaitMillis(2000);
        return config;
    }

    /**
     * @date 2021/3/19 17:29
     */
    @Bean
    public RedisStandaloneConfiguration configuration() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        //设置Database存储位置(0-15)
        redisConfig.setDatabase(0);
        redisConfig.setPassword("123456");
        redisConfig.setHostName("127.0.0.1");
        redisConfig.setPort(6379);
        return redisConfig;
    }

    @Bean("lettuceConnectionFactory")
    public LettuceConnectionFactory lettuceConnectionFactory(@Qualifier("poolConfig") GenericObjectPoolConfig config,
                                                             @Qualifier("configuration") RedisStandaloneConfiguration redisConfig) {
        //注意传入的对象名和类型RedisStandaloneConfiguration
        LettuceClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder().poolConfig(config).build();
        return new LettuceConnectionFactory(redisConfig, clientConfiguration);
    }

    /**
     * RedisTemplate配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("lettuceConnectionFactory") LettuceConnectionFactory redisConnectionFactory) {

        //配置redisTemplate
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        //RedisSerializer
        RedisSerializer<?> stringSerializer = new StringRedisSerializer();
        //设置Jackson2JsonRedisSerializer序列化, 替换默认的jdk的序列化
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        //指定要序列化的域，field,get和set,以及修饰范围，ANY是包括private和public
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //指定序列化输入的类型，类必须是非final修饰的，final修饰的类，比如String, Integer会抛出异常
        om.activateDefaultTyping(om.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.WRAPPER_ARRAY);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        //key序列化
        redisTemplate.setKeySerializer(stringSerializer);
        //value序列化
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        //Hash key序列化
        redisTemplate.setHashKeySerializer(stringSerializer);
        //Hash value序列化
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
