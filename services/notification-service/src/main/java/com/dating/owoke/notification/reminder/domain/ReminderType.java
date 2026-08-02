package com.dating.owoke.notification.reminder.domain;

import java.time.Duration;

public enum ReminderType {
    HOURS_24(Duration.ofHours(24)),
    HOURS_2(Duration.ofHours(2)),
    PERSONAL(Duration.ZERO);

    private final Duration beforeDate;

    ReminderType(Duration beforeDate) {
        this.beforeDate = beforeDate;
    }

    public Duration beforeDate() {
        return beforeDate;
    }
}
