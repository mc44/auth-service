package com.operations.auth.security;

import com.operations.auth.config.AuthSecurityProperties;
import com.operations.auth.service.RateLimitException;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {
  private static final String LOGIN_IP_PREFIX = "ratelimit:login:ip:";
  private static final String REFRESH_IP_PREFIX = "ratelimit:refresh:ip:";

  private final RateLimitStore rateLimitStore;
  private final AuthSecurityProperties properties;

  public AuthRateLimitService(RateLimitStore rateLimitStore, AuthSecurityProperties properties) {
    this.rateLimitStore = rateLimitStore;
    this.properties = properties;
  }

  public void checkLoginIpLimit(String clientIp) {
    checkIpLimit(
        LOGIN_IP_PREFIX + normalizeIp(clientIp),
        properties.getLoginRateLimitPerIp(),
        properties.getLoginRateLimitWindowSeconds());
  }

  public void checkRefreshIpLimit(String clientIp) {
    checkIpLimit(
        REFRESH_IP_PREFIX + normalizeIp(clientIp),
        properties.getRefreshRateLimitPerIp(),
        properties.getRefreshRateLimitWindowSeconds());
  }

  private void checkIpLimit(String key, int maxRequests, int windowSeconds) {
    long count = rateLimitStore.increment(key, windowSeconds);
    if (count > maxRequests) {
      throw new RateLimitException("Too many requests");
    }
  }

  private String normalizeIp(String clientIp) {
    if (clientIp == null || clientIp.isBlank()) {
      return "unknown";
    }
    return clientIp.trim();
  }
}
