package com.operations.auth.security;

public interface RateLimitStore {
  long getCount(String key);

  long increment(String key, int windowSeconds);

  void delete(String key);
}
