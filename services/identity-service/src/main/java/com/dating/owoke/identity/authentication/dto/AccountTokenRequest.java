package com.dating.owoke.identity.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountTokenRequest(@NotBlank @Size(max = 128) String token) {
}
