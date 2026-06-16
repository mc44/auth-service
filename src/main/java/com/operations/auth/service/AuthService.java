package com.operations.auth.service;

import com.operations.auth.api.dto.AuthTokenResponse;
import com.operations.auth.model.RefreshTokenDocument;
import com.operations.auth.model.RefreshTokenStatus;
import com.operations.auth.model.TenantDocument;
import com.operations.auth.model.UserDocument;
import com.operations.auth.repository.RefreshTokenRepository;
import com.operations.auth.repository.TenantRepository;
import com.operations.auth.repository.UserRepository;
import com.operations.auth.security.LoginAttemptService;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final TokenService tokenService;
  private final PasswordEncoder passwordEncoder;
  private final LoginAttemptService loginAttemptService;

  public AuthService(
      TenantRepository tenantRepository,
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      TokenService tokenService,
      PasswordEncoder passwordEncoder,
      LoginAttemptService loginAttemptService) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.tokenService = tokenService;
    this.passwordEncoder = passwordEncoder;
    this.loginAttemptService = loginAttemptService;
  }

  public AuthTokenResponse login(String tenantId, String email, String password) {
    resolveActiveTenant(tenantId);
    loginAttemptService.assertNotLocked(tenantId, email);
    UserDocument user = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
        .filter(UserDocument::isActive)
        .orElse(null);

    if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
      loginAttemptService.recordFailure(tenantId, email);
      throw new AuthException("Invalid credentials");
    }

    loginAttemptService.clearFailures(tenantId, email);
    return issueTokens(user);
  }

  @Transactional
  public AuthTokenResponse refresh(String rawRefreshToken) {
    String tokenHash = tokenService.hashRefreshToken(rawRefreshToken);
    RefreshTokenDocument current = refreshTokenRepository.findByTokenHash(tokenHash)
        .orElseThrow(() -> new AuthException("Refresh token is invalid"));

    if (current.getStatus() != RefreshTokenStatus.ACTIVE) {
      throw new AuthException("Refresh token is not active");
    }

    if (current.getExpiresAt().isBefore(Instant.now())) {
      current.setStatus(RefreshTokenStatus.EXPIRED);
      current.setUpdatedAt(Instant.now());
      refreshTokenRepository.save(current);
      throw new AuthException("Refresh token is expired");
    }

    UserDocument user = userRepository.findById(current.getUserId())
        .filter(UserDocument::isActive)
        .orElseThrow(() -> new AuthException("User no longer active"));

    if (current.getTenantId() != null && !current.getTenantId().equals(user.getTenantId())) {
      throw new AuthException("Refresh token is invalid");
    }

    String replacementRaw = tokenService.createRefreshToken();
    RefreshTokenDocument replacement = createRefreshTokenRecord(user, replacementRaw);
    refreshTokenRepository.save(replacement);

    current.setStatus(RefreshTokenStatus.ROTATED);
    current.setReplacedByTokenId(replacement.getId());
    current.setUpdatedAt(Instant.now());
    refreshTokenRepository.save(current);

    String accessToken = tokenService.createAccessToken(
        user.getId(), user.getTenantId(), user.getEmail(), user.getRoles());
    return new AuthTokenResponse(
        accessToken,
        tokenService.getAccessTokenTtlSeconds(),
        replacementRaw,
        tokenService.getRefreshTokenTtlSeconds(),
        user.getRoles());
  }

  public void logout(String rawRefreshToken) {
    String tokenHash = tokenService.hashRefreshToken(rawRefreshToken);
    refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
      token.setStatus(RefreshTokenStatus.REVOKED);
      token.setUpdatedAt(Instant.now());
      refreshTokenRepository.save(token);
    });
  }

  private TenantDocument resolveActiveTenant(String tenantId) {
    return tenantRepository.findById(tenantId)
        .filter(TenantDocument::isActive)
        .orElseThrow(() -> new AuthException("Invalid credentials"));
  }

  private AuthTokenResponse issueTokens(UserDocument user) {
    String accessToken = tokenService.createAccessToken(
        user.getId(), user.getTenantId(), user.getEmail(), user.getRoles());
    String refreshRawToken = tokenService.createRefreshToken();
    refreshTokenRepository.save(createRefreshTokenRecord(user, refreshRawToken));
    return new AuthTokenResponse(
        accessToken,
        tokenService.getAccessTokenTtlSeconds(),
        refreshRawToken,
        tokenService.getRefreshTokenTtlSeconds(),
        user.getRoles());
  }

  private RefreshTokenDocument createRefreshTokenRecord(UserDocument user, String rawRefreshToken) {
    RefreshTokenDocument token = new RefreshTokenDocument();
    token.setUserId(user.getId());
    token.setTenantId(user.getTenantId());
    token.setTokenHash(tokenService.hashRefreshToken(rawRefreshToken));
    token.setStatus(RefreshTokenStatus.ACTIVE);
    token.setExpiresAt(tokenService.refreshTokenExpiryFromNow());
    token.setCreatedAt(Instant.now());
    token.setUpdatedAt(Instant.now());
    return token;
  }
}
