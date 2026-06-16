package com.operations.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(name = "auth.redis.enabled", havingValue = "true")
public class AuthRedisConfiguration {

  @Bean
  LettuceConnectionFactory redisConnectionFactory(
      @Value("${spring.data.redis.host:localhost}") String host,
      @Value("${spring.data.redis.port:6379}") int port) {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
    return new LettuceConnectionFactory(config);
  }

  @Bean
  StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }
}
