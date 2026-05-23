package com.operations.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.operations.auth.api.dto.AuthTokenResponse;
import com.operations.auth.model.RefreshTokenDocument;
import com.operations.auth.model.RefreshTokenStatus;
import com.operations.auth.model.TenantDocument;
import com.operations.auth.model.UserDocument;
import com.operations.auth.repository.RefreshTokenRepository;
import com.operations.auth.repository.TenantRepository;
import com.operations.auth.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  private static final String TENANT = "my-app";

  @Mock
  private TenantRepository tenantRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private RefreshTokenRepository refreshTokenRepository;
  @Mock
  private PasswordEncoder passwordEncoder;

  private TokenService tokenService;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    tokenService = new TokenService(
        "change-me-in-env-please-change-me-in-env",
        "auth-service",
        900,
        1209600,
        "change-refresh-hmac-key");
    authService = new AuthService(
        tenantRepository, userRepository, refreshTokenRepository, tokenService, passwordEncoder);
  }

  @Test
  void loginReturnsTokensForValidCredentials() {
    UserDocument user = activeUser();
    when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(activeTenant()));
    when(userRepository.findByTenantIdAndEmailIgnoreCase(TENANT, "user@example.com"))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("change-me", user.getPasswordHash())).thenReturn(true);
    when(refreshTokenRepository.save(any(RefreshTokenDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AuthTokenResponse response = authService.login(TENANT, "user@example.com", "change-me");

    assertNotNull(response.accessToken());
    assertNotNull(response.refreshToken());
    assertEquals(List.of("ROLE_OPERATOR"), response.roles());
    verify(refreshTokenRepository).save(any(RefreshTokenDocument.class));
  }

  @Test
  void refreshRotatesTokenAndInvalidatesOldToken() {
    UserDocument user = activeUser();
    String oldRaw = "old.token";
    String oldHash = tokenService.hashRefreshToken(oldRaw);

    RefreshTokenDocument current = new RefreshTokenDocument();
    current.setId("old-id");
    current.setUserId(user.getId());
    current.setTenantId(TENANT);
    current.setTokenHash(oldHash);
    current.setStatus(RefreshTokenStatus.ACTIVE);
    current.setExpiresAt(Instant.now().plusSeconds(60));

    when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(current));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(refreshTokenRepository.save(any(RefreshTokenDocument.class))).thenAnswer(invocation -> {
      RefreshTokenDocument doc = invocation.getArgument(0);
      if (doc.getId() == null) {
        doc.setId("new-id");
      }
      return doc;
    });

    AuthTokenResponse rotated = authService.refresh(oldRaw);

    assertNotNull(rotated.refreshToken());
    assertNotNull(rotated.accessToken());
    assertEquals(RefreshTokenStatus.ROTATED, current.getStatus());
    assertEquals("new-id", current.getReplacedByTokenId());
  }

  @Test
  void refreshRejectsRotatedTokenReplay() {
    RefreshTokenDocument usedToken = new RefreshTokenDocument();
    usedToken.setTokenHash(tokenService.hashRefreshToken("replayed"));
    usedToken.setStatus(RefreshTokenStatus.ROTATED);
    usedToken.setExpiresAt(Instant.now().plusSeconds(300));

    when(refreshTokenRepository.findByTokenHash(usedToken.getTokenHash())).thenReturn(Optional.of(usedToken));

    assertThrows(AuthException.class, () -> authService.refresh("replayed"));
    verify(userRepository, never()).findById(any());
  }

  @Test
  void logoutRevokesExistingToken() {
    String rawToken = "logout-token";
    String hash = tokenService.hashRefreshToken(rawToken);
    RefreshTokenDocument token = new RefreshTokenDocument();
    token.setTokenHash(hash);
    token.setStatus(RefreshTokenStatus.ACTIVE);
    when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));
    when(refreshTokenRepository.save(any(RefreshTokenDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    authService.logout(rawToken);

    ArgumentCaptor<RefreshTokenDocument> captor = ArgumentCaptor.forClass(RefreshTokenDocument.class);
    verify(refreshTokenRepository).save(captor.capture());
    assertEquals(RefreshTokenStatus.REVOKED, captor.getValue().getStatus());
  }

  private UserDocument activeUser() {
    UserDocument user = new UserDocument();
    user.setId("user-1");
    user.setTenantId(TENANT);
    user.setEmail("user@example.com");
    user.setPasswordHash("encoded");
    user.setRoles(List.of("ROLE_OPERATOR"));
    user.setActive(true);
    return user;
  }

  private TenantDocument activeTenant() {
    TenantDocument tenant = new TenantDocument();
    tenant.setId(TENANT);
    tenant.setActive(true);
    return tenant;
  }
}
