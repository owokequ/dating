package com.dating.owoke.identity.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(min = 12, max = 72) String password) {

    public RegisterRequest {
        email = email == null ? null : email.trim();
        displayName = displayName == null ? null : displayName.trim();
    }
}
