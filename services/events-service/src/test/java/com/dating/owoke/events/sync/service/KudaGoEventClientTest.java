package com.dating.owoke.events.sync.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KudaGoEventClientTest {
    @Test
    void normalizesOnlyKudaGoHttpsLinks() {
        assertThat(KudaGoEventClient.normalizeKudaGoUrl("http://kudago.com/kzn/event/test/"))
                .isEqualTo("https://kudago.com/kzn/event/test/");
        assertThat(KudaGoEventClient.normalizeKudaGoUrl("https://kzn.kudago.com/event/test/"))
                .isEqualTo("https://kzn.kudago.com/event/test/");
        assertThat(KudaGoEventClient.normalizeKudaGoUrl("https://fake-kudago.com/event/test/")).isNull();
        assertThat(KudaGoEventClient.normalizeKudaGoUrl("https://example.com/event/test/")).isNull();
    }

    @Test
    void acceptsOnlyKudaGoMediaPathsForImages() {
        assertThat(KudaGoEventClient.normalizeKudaGoMediaUrl("http://kudago.com/media/images/test.jpg"))
                .isEqualTo("https://kudago.com/media/images/test.jpg");
        assertThat(KudaGoEventClient.normalizeKudaGoMediaUrl("https://kudago.com/kzn/event/test/")).isNull();
    }
}
