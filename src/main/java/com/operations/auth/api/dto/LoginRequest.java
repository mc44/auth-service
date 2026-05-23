package com.operations.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String tenantId,
    @Email @NotBlank String email,
    @NotBlank String password
) {}
