package com.dating.owoke.media.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PrivateDateDraftProjectionTest {

    @Test
    void acceptsUploadOnlyFromDraftAuthorBeforeExpiry() {
        UUID author = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-03T10:00:00Z");
        PrivateDateDraftProjection draft = new PrivateDateDraftProjection(
                UUID.randomUUID(), author, now.plusSeconds(60));

        assertThat(draft.canUpload(author, now)).isTrue();
        assertThat(draft.canUpload(UUID.randomUUID(), now)).isFalse();
        assertThat(draft.canUpload(author, now.plusSeconds(61))).isFalse();
    }
}
