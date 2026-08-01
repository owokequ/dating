package com.dating.owoke.media.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dating.owoke.media.collection.domain.MediaOwnerType;

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

    @Test
    void suppressedRemoteAssetCanOnlyReturnThroughExplicitProviderRestore() {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        MediaAsset asset = MediaAsset.remote(
                UUID.randomUUID(), MediaOwnerType.EVENT, UUID.randomUUID(), "KUDAGO", "image-1",
                "https://kudago.com/media/images/one.jpg", "KudaGo",
                "https://kudago.com/events/one/", now);

        asset.suppressRemote(now.plusSeconds(1));
        assertThat(asset.refreshRemote(
                "https://kudago.com/media/images/two.jpg", "KudaGo", "https://kudago.com/events/one/"))
                .isFalse();

        asset.restoreRemote(
                "https://kudago.com/media/images/two.jpg", "KudaGo",
                "https://kudago.com/events/one/", now.plusSeconds(2));

        assertThat(asset.getStatus()).isEqualTo(MediaAssetStatus.READY);
        assertThat(asset.getRemoteUrl()).endsWith("two.jpg");
    }
}
