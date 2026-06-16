package com.operations.auth.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "auth.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimitStore implements RateLimitStore {
  private record Counter(long count, Instant expiresAt) {}

  private final Map<String, Counter> counters = new ConcurrentHashMap<>();

  @Override
  public long getCount(String key) {
    Counter existing = counters.get(key);
    if (existing == null || existing.expiresAt().isBefore(Instant.now())) {
      return 0;
    }
    return existing.count();
  }

  @Override
  public long increment(String key, int windowSeconds) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(windowSeconds);
    Counter updated = counters.compute(key, (k, existing) -> {
      if (existing == null || existing.expiresAt().isBefore(now)) {
        return new Counter(1, expiresAt);
      }
      return new Counter(existing.count() + 1, existing.expiresAt());
    });
    return updated.count();
  }

  @Override
  public void delete(String key) {
    counters.remove(key);
  }
}
