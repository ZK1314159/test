package com.test.config.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class CacheAbleConfig {

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory factory) {
    // 配置 JSON 序列化器
    Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
    ObjectMapper om = new ObjectMapper();
    om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
    serializer.setObjectMapper(om);
    // 配置缓存配置对象
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
    config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    config = config.computePrefixWith(cacheName -> cacheName + "_");
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    cacheConfigurations.put("course", config.entryTtl(Duration.ofSeconds(120)));
    // 创建 CacheManager
    return RedisCacheManager.builder(factory)
      .cacheDefaults(config)
      .initialCacheNames(cacheConfigurations.keySet())
      .withInitialCacheConfigurations(cacheConfigurations)
      .build();
  }

  @Bean("myKeyGenerator")
  public KeyGenerator keyGenerator() {
    return (Object target, Method method, Object... params) -> params[0].toString();
  }

}

