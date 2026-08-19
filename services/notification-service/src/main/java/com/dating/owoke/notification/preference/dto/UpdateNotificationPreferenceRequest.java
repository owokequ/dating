package com.dating.owoke.notification.preference.dto;
public record UpdateNotificationPreferenceRequest(boolean inAppEnabled, boolean pushEnabled, boolean telegramEnabled, boolean emailEnabled) { }
