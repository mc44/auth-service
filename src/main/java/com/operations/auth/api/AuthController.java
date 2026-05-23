package com.operations.auth.api;

import com.operations.auth.api.dto.AuthTokenResponse;
import com.operations.auth.api.dto.LoginRequest;
import com.operations.auth.api.dto.LogoutRequest;
import com.operations.auth.api.dto.RefreshRequest;
import com.operations.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(
        authService.login(request.tenantId(), request.email(), request.password()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return ResponseEntity.ok(authService.refresh(request.refreshToken()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request.refreshToken());
    return ResponseEntity.noContent().build();
  }
}
