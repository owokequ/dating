package com.dating.owoke.notification.push.dto;

import com.dating.owoke.notification.push.domain.MobilePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterDeviceRequest(
        @NotBlank @Pattern(regexp = "^(ExponentPushToken|ExpoPushToken)\\[.+]$") String expoPushToken,
        @NotNull MobilePlatform platform) { }
