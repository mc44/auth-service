package com.operations.auth.security;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "auth.redis.enabled", havingValue = "true")
public class RedisRateLimitStore implements RateLimitStore {
  private final StringRedisTemplate redisTemplate;

  public RedisRateLimitStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public long getCount(String key) {
    String value = redisTemplate.opsForValue().get(key);
    if (value == null) {
      return 0;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  @Override
  public long increment(String key, int windowSeconds) {
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1) {
      redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
    }
    return count != null ? count : 0;
  }

  @Override
  public void delete(String key) {
    redisTemplate.delete(key);
  }
}
