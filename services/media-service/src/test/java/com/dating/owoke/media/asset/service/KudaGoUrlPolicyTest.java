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
        assertThat(policy.imageUrl("https://media.kudago.com/images/event/event.jpg"))
                .isEqualTo("https://media.kudago.com/images/event/event.jpg");
        assertThat(policy.imageUrl("https://media.kudago.com/thumbs/xl/images/event/event.jpg"))
                .isEqualTo("https://media.kudago.com/thumbs/xl/images/event/event.jpg");
    }

    @Test
    void rejectsForeignHostAndNonMediaPath() {
        assertThatThrownBy(() -> policy.imageUrl("https://evil.example/media/images/event.jpg"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.imageUrl("https://kudago.com/events/event"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.imageUrl("https://static.kudago.com/img/logo.svg"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsOptionalExternalAttributionLinkWithoutUsingItAsMediaUrl() {
        assertThat(policy.sourceLink(null)).isNull();
        assertThat(policy.sourceLink("https://press.example/photo-author"))
                .isEqualTo("https://press.example/photo-author");
        assertThat(policy.sourceLink("https://press.example/gallery#photo-1"))
                .isEqualTo("https://press.example/gallery#photo-1");
        assertThatThrownBy(() -> policy.sourceLink("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
