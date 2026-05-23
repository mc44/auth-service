package com.operations.auth.api.dto;

import java.util.List;

public record AuthTokenResponse(
    String accessToken,
    long accessTokenExpiresInSeconds,
    String refreshToken,
    long refreshTokenExpiresInSeconds,
    List<String> roles
) {}
