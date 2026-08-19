package com.dating.owoke.identity.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileRefreshTokenRequest(
        @NotBlank @Size(max = 512) String refreshToken) {
}
