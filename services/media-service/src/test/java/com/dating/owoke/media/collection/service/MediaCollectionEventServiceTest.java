package com.dating.owoke.media.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dating.owoke.media.asset.dto.MediaItemResponse;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.messaging.event.MediaCollectionChangedV2;
import com.dating.owoke.media.shared.messaging.service.OutboxService;

class MediaCollectionEventServiceTest {

    @Test
    void eventCollectionPublishesVersionTwoContract() {
        OutboxService outboxService = mock(OutboxService.class);
        MediaCollectionEventService service = new MediaCollectionEventService(outboxService);
        UUID eventId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        MediaItemResponse item = new MediaItemResponse(
                mediaId, "REMOTE_URL", "image-1", "READY", 0, true,
                "https://kudago.com/media/images/one.jpg",
                "https://kudago.com/media/images/one.jpg",
                "https://kudago.com/media/images/one.jpg",
                "KudaGo", "https://kudago.com/events/one/");

        service.collectionChanged(new MediaCollectionSnapshot(
                MediaOwnerType.EVENT, eventId, mediaId, List.of(mediaId), List.of(item), 3));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).enqueue(
                eq("media.events.v1"), eq(eventId), eq("MediaCollectionChangedV2"), eq(2), payload.capture());
        MediaCollectionChangedV2 changed = (MediaCollectionChangedV2) payload.getValue();
        assertThat(changed.ownerType()).isEqualTo("EVENT");
        assertThat(changed.orderedMediaIds()).containsExactly(mediaId);
        assertThat(changed.items().getFirst().providerAssetKey()).isEqualTo("image-1");
    }
}
