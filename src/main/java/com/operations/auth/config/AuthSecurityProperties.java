package com.operations.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.security")
public class AuthSecurityProperties {
  private int loginMaxAttempts = 5;
  private int loginLockoutSeconds = 900;
  private int loginFailureWindowSeconds = 900;
  private int loginRateLimitPerIp = 10;
  private int loginRateLimitWindowSeconds = 60;
  private int refreshRateLimitPerIp = 30;
  private int refreshRateLimitWindowSeconds = 60;

  public int getLoginMaxAttempts() {
    return loginMaxAttempts;
  }

  public void setLoginMaxAttempts(int loginMaxAttempts) {
    this.loginMaxAttempts = loginMaxAttempts;
  }

  public int getLoginLockoutSeconds() {
    return loginLockoutSeconds;
  }

  public void setLoginLockoutSeconds(int loginLockoutSeconds) {
    this.loginLockoutSeconds = loginLockoutSeconds;
  }

  public int getLoginFailureWindowSeconds() {
    return loginFailureWindowSeconds;
  }

  public void setLoginFailureWindowSeconds(int loginFailureWindowSeconds) {
    this.loginFailureWindowSeconds = loginFailureWindowSeconds;
  }

  public int getLoginRateLimitPerIp() {
    return loginRateLimitPerIp;
  }

  public void setLoginRateLimitPerIp(int loginRateLimitPerIp) {
    this.loginRateLimitPerIp = loginRateLimitPerIp;
  }

  public int getLoginRateLimitWindowSeconds() {
    return loginRateLimitWindowSeconds;
  }

  public void setLoginRateLimitWindowSeconds(int loginRateLimitWindowSeconds) {
    this.loginRateLimitWindowSeconds = loginRateLimitWindowSeconds;
  }

  public int getRefreshRateLimitPerIp() {
    return refreshRateLimitPerIp;
  }

  public void setRefreshRateLimitPerIp(int refreshRateLimitPerIp) {
    this.refreshRateLimitPerIp = refreshRateLimitPerIp;
  }

  public int getRefreshRateLimitWindowSeconds() {
    return refreshRateLimitWindowSeconds;
  }

  public void setRefreshRateLimitWindowSeconds(int refreshRateLimitWindowSeconds) {
    this.refreshRateLimitWindowSeconds = refreshRateLimitWindowSeconds;
  }
}
