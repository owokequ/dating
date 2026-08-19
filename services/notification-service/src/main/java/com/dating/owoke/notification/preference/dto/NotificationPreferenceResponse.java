package com.dating.owoke.notification.preference.dto;
public record NotificationPreferenceResponse(boolean inAppEnabled, boolean pushEnabled, boolean telegramEnabled, boolean emailEnabled) { }
