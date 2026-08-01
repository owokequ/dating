package com.dating.owoke.media.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MediaAssetTest {

    @Test
    void followsExplicitProcessingAndDeletionLifecycle() {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        MediaAsset asset = new MediaAsset(
                UUID.randomUUID(), MediaAssetSource.UPLOAD, "place.jpg", 100, UUID.randomUUID(), now);

        asset.markProcessing();
        asset.markReady("image/jpeg", 1200, 800, "a".repeat(64), now.plusSeconds(1));
        asset.softDelete(now.plusSeconds(2), now.plus(30, ChronoUnit.DAYS));
        asset.markPurged(now.plus(31, ChronoUnit.DAYS));

        assertThat(asset.getStatus()).isEqualTo(MediaAssetStatus.DELETED);
        assertThat(asset.getPurgedAt()).isEqualTo(now.plus(31, ChronoUnit.DAYS));
    }

    @Test
    void cannotBecomeReadyBeforeItIsClaimedForProcessing() {
        MediaAsset asset = new MediaAsset(
                UUID.randomUUID(), MediaAssetSource.UPLOAD, "place.jpg", 100, UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> asset.markReady("image/jpeg", 100, 100, "a".repeat(64), Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}
