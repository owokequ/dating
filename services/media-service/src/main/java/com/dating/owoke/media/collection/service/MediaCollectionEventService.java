package com.dating.owoke.media.collection.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.domain.MediaVariant;
import com.dating.owoke.media.asset.messaging.event.MediaAssetDeletedV1;
import com.dating.owoke.media.asset.messaging.event.MediaAssetReadyV1;
import com.dating.owoke.media.asset.messaging.event.MediaVariantV1;
import com.dating.owoke.media.collection.messaging.event.MediaCollectionChangedV1;
import com.dating.owoke.media.collection.messaging.event.MediaCollectionChangedV2;
import com.dating.owoke.media.collection.messaging.event.MediaCollectionItemV2;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.shared.messaging.service.OutboxService;

@Service
public class MediaCollectionEventService {

    private static final String MEDIA_EVENTS_TOPIC = "media.events.v1";

    private final OutboxService outboxService;

    public MediaCollectionEventService(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    public void assetReady(MediaOwnerType ownerType, UUID ownerId, MediaAsset asset, List<MediaVariant> variants) {
        List<MediaVariantV1> payloadVariants = variants.stream()
                .map(variant -> new MediaVariantV1(
                        variant.getVariant().name(),
                        variant.getContentType(),
                        variant.getWidth(),
                        variant.getHeight(),
                        variant.getSize(),
                        variant.getSha256()))
                .toList();
        if (ownerType == MediaOwnerType.PLACE) {
            outboxService.enqueue(
                    MEDIA_EVENTS_TOPIC,
                    ownerId,
                    "MediaAssetReadyV1",
                    new MediaAssetReadyV1(asset.getId(), ownerType.name(), ownerId, asset.getSha256(), payloadVariants));
        }
    }

    public void collectionChanged(MediaCollectionSnapshot snapshot) {
        if (snapshot.ownerType() == MediaOwnerType.PLACE) {
            outboxService.enqueue(
                MEDIA_EVENTS_TOPIC,
                snapshot.ownerId(),
                "MediaCollectionChangedV1",
                new MediaCollectionChangedV1(
                        "PLACE",
                        snapshot.ownerId(),
                        snapshot.coverMediaId(),
                        snapshot.orderedMediaIds(),
                        snapshot.revision()));
        }
        outboxService.enqueue(
                MEDIA_EVENTS_TOPIC,
                snapshot.ownerId(),
                "MediaCollectionChangedV2",
                2,
                new MediaCollectionChangedV2(
                        snapshot.ownerType().name(),
                        snapshot.ownerId(),
                        snapshot.coverMediaId(),
                        snapshot.orderedMediaIds(),
                        snapshot.items().stream().map(item -> new MediaCollectionItemV2(
                                item.mediaId(), item.position(), item.source(), item.providerAssetKey(),
                                item.thumbnailUrl(), item.cardUrl(),
                                item.detailUrl(), item.sourceName(), item.sourceLink())).toList(),
                        snapshot.revision()));
    }

    public void assetDeleted(MediaOwnerType ownerType, UUID ownerId, UUID mediaId, Instant deletedAt, Instant purgeAfter) {
        outboxService.enqueue(
                MEDIA_EVENTS_TOPIC,
                ownerId,
                "MediaAssetDeletedV1",
                new MediaAssetDeletedV1(mediaId, ownerType.name(), ownerId, deletedAt, purgeAfter));
    }
}
