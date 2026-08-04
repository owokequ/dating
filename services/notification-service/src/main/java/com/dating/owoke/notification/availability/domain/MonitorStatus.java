package com.dating.owoke.notification.availability.domain;

public enum MonitorStatus {
    UNKNOWN,
    UP,
    DOWN;

    public static MonitorStatus fromExternal(String value) {
        if (value == null) {
            throw new IllegalArgumentException("External monitor status must not be null");
        }
        return switch (value.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "UP", "2" -> UP;
            case "DOWN", "1" -> DOWN;
            default -> throw new IllegalArgumentException("Unsupported external monitor status");
        };
    }
}
