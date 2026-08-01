package com.dating.owoke.media.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KudaGoUrlPolicyTest {

    private final KudaGoUrlPolicy policy = new KudaGoUrlPolicy();

    @Test
    void normalizesAllowedKudaGoImageToHttps() {
        assertThat(policy.imageUrl("http://kudago.com/media/images/event.jpg"))
                .isEqualTo("https://kudago.com/media/images/event.jpg");
    }

    @Test
    void rejectsForeignHostAndNonMediaPath() {
        assertThatThrownBy(() -> policy.imageUrl("https://evil.example/media/images/event.jpg"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.imageUrl("https://kudago.com/events/event"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
