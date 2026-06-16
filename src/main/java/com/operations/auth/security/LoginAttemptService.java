package com.operations.auth.security;

import com.operations.auth.config.AuthSecurityProperties;
import com.operations.auth.service.AuthException;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {
  private static final String FAILURE_PREFIX = "login:fail:";

  private final RateLimitStore rateLimitStore;
  private final AuthSecurityProperties properties;

  public LoginAttemptService(RateLimitStore rateLimitStore, AuthSecurityProperties properties) {
    this.rateLimitStore = rateLimitStore;
    this.properties = properties;
  }

  public void assertNotLocked(String tenantId, String email) {
    String key = failureKey(tenantId, email);
    long failures = rateLimitStore.getCount(key);
    if (failures >= properties.getLoginMaxAttempts()) {
      throw new AuthException("Invalid credentials");
    }
  }

  public void recordFailure(String tenantId, String email) {
    rateLimitStore.increment(failureKey(tenantId, email), properties.getLoginLockoutSeconds());
  }

  public void clearFailures(String tenantId, String email) {
    rateLimitStore.delete(failureKey(tenantId, email));
  }

  private String failureKey(String tenantId, String email) {
    return FAILURE_PREFIX + tenantId + ":" + email.toLowerCase(Locale.ROOT);
  }
}
