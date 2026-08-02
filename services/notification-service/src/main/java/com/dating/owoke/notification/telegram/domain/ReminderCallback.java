package com.dating.owoke.notification.telegram.domain;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public record ReminderCallback(String action, UUID proposalId) {
    private static final String PREFIX = "rem";

    public ReminderCallback {
        if (!java.util.Set.of("3h", "1h", "30m", "pick", "15m", "45m", "90m", "text", "edit", "off").contains(action)) {
            throw new IllegalArgumentException("Unsupported reminder action");
        }
    }

    public String encode() {
        ByteBuffer bytes = ByteBuffer.allocate(16).putLong(proposalId.getMostSignificantBits()).putLong(proposalId.getLeastSignificantBits());
        return PREFIX + ":" + action + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array());
    }

    public static boolean isReminder(String value) { return value != null && value.startsWith(PREFIX + ":"); }

    public static ReminderCallback decode(String value) {
        String[] parts = value == null ? new String[0] : value.split(":", 3);
        if (parts.length != 3 || !PREFIX.equals(parts[0])) throw new IllegalArgumentException("Invalid reminder callback");
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(parts[2]);
            if (bytes.length != 16) throw new IllegalArgumentException("Invalid compact UUID length");
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new ReminderCallback(parts[1], new UUID(buffer.getLong(), buffer.getLong()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid reminder callback", exception);
        }
    }
}
