package com.dating.owoke.media.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlaceProjectionTest {

    @Test
    void draftAcceptsAdminUploadsButIsNotPublic() {
        var projection = new PlaceProjection(UUID.randomUUID(), PlaceProjectionStatus.DRAFT, Instant.now());

        assertThat(projection.acceptsUploads()).isTrue();
        assertThat(projection.isActive()).isFalse();
    }

    @Test
    void archivedProjectionRejectsUploads() {
        var projection = new PlaceProjection(UUID.randomUUID(), PlaceProjectionStatus.ARCHIVED, Instant.now());

        assertThat(projection.acceptsUploads()).isFalse();
        assertThat(projection.isActive()).isFalse();
    }
}
