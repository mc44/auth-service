package com.operations.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.operations.auth.api.dto.ErrorResponse;
import com.operations.auth.service.RateLimitException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
  private final AuthRateLimitService authRateLimitService;
  private final ObjectMapper objectMapper;

  public AuthRateLimitFilter(AuthRateLimitService authRateLimitService, ObjectMapper objectMapper) {
    this.authRateLimitService = authRateLimitService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    String path = request.getRequestURI();
    String clientIp = resolveClientIp(request);
    try {
      if (path.endsWith("/auth/login")) {
        authRateLimitService.checkLoginIpLimit(clientIp);
      } else if (path.endsWith("/auth/refresh")) {
        authRateLimitService.checkRefreshIpLimit(clientIp);
      }
      filterChain.doFilter(request, response);
    } catch (RateLimitException ex) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setHeader("Retry-After", "60");
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(
          response.getOutputStream(),
          new ErrorResponse("RATE_LIMITED", ex.getMessage(), Instant.now()));
    }
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
