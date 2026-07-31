package com.dating.owoke.notification.telegram.domain;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record DateProposalCallback(UUID proposalId, UUID coupleId, String decision) {

    private static final String PREFIX = "date";

    public DateProposalCallback {
        Objects.requireNonNull(proposalId, "proposalId must not be null");
        Objects.requireNonNull(coupleId, "coupleId must not be null");
        decision = Objects.requireNonNull(decision, "decision must not be null").toUpperCase(Locale.ROOT);
        if (!"ACCEPT".equals(decision) && !"DECLINE".equals(decision)) {
            throw new IllegalArgumentException("Unsupported date proposal decision");
        }
    }

    public String encode() {
        String action = "ACCEPT".equals(decision) ? "a" : "d";
        return String.join(":", PREFIX, action, encodeUuid(proposalId), encodeUuid(coupleId));
    }

    public static DateProposalCallback decode(String value) {
        String[] parts = value == null ? new String[0] : value.split(":", 4);
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Invalid Telegram date callback");
        }
        String decision = switch (parts[1]) {
            case "a" -> "ACCEPT";
            case "d" -> "DECLINE";
            default -> throw new IllegalArgumentException("Unsupported date proposal decision");
        };
        return new DateProposalCallback(decodeUuid(parts[2]), decodeUuid(parts[3]), decision);
    }

    private static String encodeUuid(UUID value) {
        ByteBuffer bytes = ByteBuffer.allocate(16)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array());
    }

    private static UUID decodeUuid(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            if (bytes.length != 16) {
                throw new IllegalArgumentException("Invalid compact UUID length");
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid compact UUID", exception);
        }
    }
}
