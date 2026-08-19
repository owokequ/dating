package com.dating.owoke.identity.telegram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TelegramMobileExchangeRequest(
        @NotBlank @Size(max = 512) String code) {
}
