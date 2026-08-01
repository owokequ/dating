package com.dating.owoke.media.collection.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.collection.domain.MediaCollection;
import com.dating.owoke.media.collection.domain.MediaCollectionItem;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.dto.ReorderMediaCollectionRequest;
import com.dating.owoke.media.collection.repository.MediaCollectionItemRepository;
import com.dating.owoke.media.collection.repository.MediaCollectionRepository;
import com.dating.owoke.media.processing.configuration.MediaProcessingProperties;
import com.dating.owoke.media.shared.exception.BusinessConflictException;
import com.dating.owoke.media.shared.exception.ResourceNotFoundException;

import jakarta.persistence.EntityManager;

@Service
public class MediaCollectionCommandService {

    private static final int TEMPORARY_POSITION_OFFSET = 100;

    private final MediaCollectionRepository collectionRepository;
    private final MediaCollectionItemRepository itemRepository;
    private final MediaAssetRepository assetRepository;
    private final MediaCollectionSnapshotService snapshotService;
    private final MediaCollectionEventService eventService;
    private final MediaProcessingProperties properties;
    private final EntityManager entityManager;
    private final Clock clock;

    public MediaCollectionCommandService(
            MediaCollectionRepository collectionRepository,
            MediaCollectionItemRepository itemRepository,
            MediaAssetRepository assetRepository,
            MediaCollectionSnapshotService snapshotService,
            MediaCollectionEventService eventService,
            MediaProcessingProperties properties,
            EntityManager entityManager,
            Clock clock) {
        this.collectionRepository = collectionRepository;
        this.itemRepository = itemRepository;
        this.assetRepository = assetRepository;
        this.snapshotService = snapshotService;
        this.eventService = eventService;
        this.properties = properties;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public void reorder(UUID placeId, ReorderMediaCollectionRequest request) {
        MediaCollection collection = requiredCollection(placeId);
        List<MediaCollectionItem> items = itemRepository.lockActive(MediaOwnerType.PLACE, placeId);
        validateOrder(items, request);
        Map<UUID, MediaCollectionItem> byMediaId = items.stream()
                .collect(Collectors.toMap(MediaCollectionItem::getMediaAssetId, Function.identity()));
        moveToTemporaryPositions(items);
        for (int position = 0; position < request.orderedMediaIds().size(); position++) {
            UUID mediaId = request.orderedMediaIds().get(position);
            byMediaId.get(mediaId).reorder(position, mediaId.equals(request.coverMediaId()));
        }
        collection.changed(clock.instant());
        entityManager.flush();
        eventService.collectionChanged(snapshotService.readySnapshot(placeId));
    }

    @Transactional
    public void delete(UUID placeId, UUID mediaId) {
        MediaCollection collection = requiredCollection(placeId);
        List<MediaCollectionItem> items = itemRepository.lockActive(MediaOwnerType.PLACE, placeId);
        MediaCollectionItem removed = items.stream()
                .filter(item -> item.getMediaAssetId().equals(mediaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image does not belong to this place"));
        MediaAsset asset = assetRepository.lockById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset is missing"));
        Instant now = clock.instant();
        Instant purgeAfter = now.plus(properties.purgeDelay());
        boolean removedCover = removed.isCover();
        removed.delete(now);
        asset.softDelete(now, purgeAfter);

        List<MediaCollectionItem> remaining = items.stream().filter(item -> item != removed).toList();
        boolean needsNewCover = removedCover || remaining.stream().noneMatch(MediaCollectionItem::isCover);
        moveToTemporaryPositions(remaining);
        for (int position = 0; position < remaining.size(); position++) {
            MediaCollectionItem item = remaining.get(position);
            item.reorder(position, needsNewCover ? position == 0 : item.isCover());
        }
        collection.changed(now);
        entityManager.flush();
        eventService.assetDeleted(placeId, mediaId, now, purgeAfter);
        eventService.collectionChanged(snapshotService.readySnapshot(placeId));
    }

    private MediaCollection requiredCollection(UUID placeId) {
        return collectionRepository.lockByOwnerId(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place media collection does not exist"));
    }

    private void validateOrder(List<MediaCollectionItem> items, ReorderMediaCollectionRequest request) {
        if (new HashSet<>(request.orderedMediaIds()).size() != request.orderedMediaIds().size()) {
            throw new BusinessConflictException("Media order contains duplicate identifiers");
        }
        var current = items.stream().map(MediaCollectionItem::getMediaAssetId).collect(Collectors.toSet());
        var requested = new HashSet<>(request.orderedMediaIds());
        if (!current.equals(requested)) {
            throw new BusinessConflictException("Media order must contain every active image exactly once");
        }
        if (!requested.contains(request.coverMediaId())) {
            throw new BusinessConflictException("Cover image must belong to the collection");
        }
    }

    private void moveToTemporaryPositions(List<MediaCollectionItem> items) {
        for (int index = 0; index < items.size(); index++) {
            items.get(index).reorder(TEMPORARY_POSITION_OFFSET + index, items.get(index).isCover());
        }
        entityManager.flush();
    }
}
