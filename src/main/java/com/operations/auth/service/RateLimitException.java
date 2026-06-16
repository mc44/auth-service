package com.operations.auth.service;

public class RateLimitException extends RuntimeException {
  public RateLimitException(String message) {
    super(message);
  }
}
