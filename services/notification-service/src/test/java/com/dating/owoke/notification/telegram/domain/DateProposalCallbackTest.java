package com.dating.owoke.notification.telegram.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DateProposalCallbackTest {

    @Test
    void roundTripKeepsBothIdentifiersWithinTelegramLimit() {
        DateProposalCallback callback = new DateProposalCallback(
                UUID.randomUUID(), UUID.randomUUID(), "ACCEPT");

        String encoded = callback.encode();

        assertThat(encoded.getBytes(StandardCharsets.UTF_8)).hasSizeLessThanOrEqualTo(64);
        assertThat(DateProposalCallback.decode(encoded)).isEqualTo(callback);
    }

    @Test
    void malformedCallbackIsRejected() {
        assertThatThrownBy(() -> DateProposalCallback.decode("date:a:not-a-uuid:also-invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
