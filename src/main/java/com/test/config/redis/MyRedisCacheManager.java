//package com.test.config.redis;
//
//import com.fasterxml.jackson.annotation.JsonAutoDetect;
//import com.fasterxml.jackson.annotation.PropertyAccessor;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.RedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//import java.time.Duration;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Configuration
//@EnableCaching
//public class MyRedisCacheManager {
//
//  @Bean
//  public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
//    RedisSerializer<String> redisSerializer = new StringRedisSerializer();
//    Jackson2JsonRedisSerializer<?> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
//    ObjectMapper objectMapper = new ObjectMapper();
//    objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//    jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
//
//    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
//      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
//
//    Set<String> cacheNames = new HashSet<>();
//    ConcurrentHashMap<String, RedisCacheConfiguration> configMap = new ConcurrentHashMap<>();
//    //需要先初始化缓存名称，再初始化其它的配置。
//    return RedisCacheManager.builder(factory).initialCacheNames(cacheNames).withInitialCacheConfigurations(configMap).build();
//  }
//
//  @Bean
//  public RedisCacheManager cacheManager2(RedisConnectionFactory factory) {
//
//    RedisSerializer<String> redisSerializer = new StringRedisSerializer();
//    Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
//    ObjectMapper objectMapper = new ObjectMapper();
//    objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//    objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//    jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
//
//    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
//      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
//
//    Set cacheNames = new HashSet<>();
//    cacheNames.add("car");
//    cacheNames.add("distributor");
//
//    ConcurrentHashMap configMap = new ConcurrentHashMap<>();
//    //有效期6分钟自定义缓存时间
//    configMap.put("car", config.entryTtl(Duration.ofMinutes(1L)));
//    //永久 key1 的有效期是永久的
//    configMap.put("distributor", config);
//    //需要先初始化缓存名称，再初始化其它的配置。
//    RedisCacheManager cacheManager = RedisCacheManager.builder(factory).initialCacheNames(cacheNames).withInitialCacheConfigurations(configMap).build();
//    return cacheManager;
//  }
//
//}
//
