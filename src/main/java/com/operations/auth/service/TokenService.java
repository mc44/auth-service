package com.operations.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private final String jwtIssuer;
  private final long accessTokenTtlSeconds;
  private final long refreshTokenTtlSeconds;
  private final String refreshTokenHmacKey;
  private final SecretKey jwtSecretKey;

  public TokenService(
      @Value("${auth.jwt.secret}") String jwtSecret,
      @Value("${auth.jwt.issuer}") String jwtIssuer,
      @Value("${auth.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds,
      @Value("${auth.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds,
      @Value("${auth.jwt.refresh-token-hmac-key}") String refreshTokenHmacKey) {
    this.jwtIssuer = jwtIssuer;
    this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    this.refreshTokenHmacKey = refreshTokenHmacKey;
    this.jwtSecretKey = resolveSecretKey(jwtSecret);
  }

  public String createAccessToken(String userId, String tenantId, String email, List<String> roles) {
    Instant now = Instant.now();
    Instant expiry = now.plusSeconds(accessTokenTtlSeconds);
    return Jwts.builder()
        .issuer(jwtIssuer)
        .subject(userId)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .claim("tenantId", tenantId)
        .claim("email", email)
        .claim("roles", roles)
        .signWith(jwtSecretKey)
        .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(jwtSecretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public String createRefreshToken() {
    return UUID.randomUUID() + "." + UUID.randomUUID();
  }

  public String hashRefreshToken(String rawRefreshToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((refreshTokenHmacKey + ":" + rawRefreshToken).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Unable to hash refresh token", ex);
    }
  }

  public Instant accessTokenExpiryFromNow() {
    return Instant.now().plusSeconds(accessTokenTtlSeconds);
  }

  public Instant refreshTokenExpiryFromNow() {
    return Instant.now().plusSeconds(refreshTokenTtlSeconds);
  }

  public long getAccessTokenTtlSeconds() {
    return accessTokenTtlSeconds;
  }

  public long getRefreshTokenTtlSeconds() {
    return refreshTokenTtlSeconds;
  }

  private SecretKey resolveSecretKey(String rawSecret) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] keyMaterial = digest.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
      return Keys.hmacShaKeyFor(keyMaterial);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Unable to create JWT signing key", ex);
    }
  }
}
