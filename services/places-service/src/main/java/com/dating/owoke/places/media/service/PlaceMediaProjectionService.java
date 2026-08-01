package com.dating.owoke.places.media.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.places.media.domain.PlaceMediaCollection;
import com.dating.owoke.places.media.domain.PlaceMediaProjectionItem;
import com.dating.owoke.places.media.repository.PlaceMediaCollectionRepository;
import com.dating.owoke.places.media.repository.PlaceMediaProjectionItemRepository;
import com.dating.owoke.places.media.messaging.event.MediaCollectionChangedV2;

@Service
public class PlaceMediaProjectionService {

    private final PlaceMediaCollectionRepository collectionRepository;
    private final PlaceMediaProjectionItemRepository itemRepository;

    public PlaceMediaProjectionService(
            PlaceMediaCollectionRepository collectionRepository,
            PlaceMediaProjectionItemRepository itemRepository) {
        this.collectionRepository = collectionRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public void replace(
            UUID placeId,
            UUID coverMediaId,
            List<UUID> orderedMediaIds,
            long revision,
            Instant occurredAt) {
        PlaceMediaCollection collection = collectionRepository.findById(placeId).orElse(null);
        if (collection != null && !collection.updateIfNewer(revision, occurredAt)) {
            return;
        }
        if (collection == null) {
            collection = new PlaceMediaCollection(placeId, revision, occurredAt);
        }
        itemRepository.deleteByPlaceId(placeId);
        itemRepository.flush();
        for (int position = 0; position < orderedMediaIds.size(); position++) {
            UUID mediaId = orderedMediaIds.get(position);
            String base = "/api/v1/media/assets/" + mediaId + "/content?variant=";
            itemRepository.save(new PlaceMediaProjectionItem(
                    placeId, mediaId, position, mediaId.equals(coverMediaId),
                    "UPLOAD", null, base + "THUMBNAIL", base + "CARD", base + "DETAIL", null, null, occurredAt));
        }
        collectionRepository.save(collection);
    }

    @Transactional
    public void replaceV2(
            UUID placeId,
            UUID coverMediaId,
            List<MediaCollectionChangedV2.Item> items,
            long revision,
            Instant occurredAt) {
        PlaceMediaCollection collection = collectionRepository.findById(placeId).orElse(null);
        if (collection != null && revision < collection.getRevision()) {
            return;
        } else if (collection != null && revision > collection.getRevision()) {
            collection.updateIfNewer(revision, occurredAt);
        } else if (collection == null) {
            collection = new PlaceMediaCollection(placeId, revision, occurredAt);
        }
        itemRepository.deleteByPlaceId(placeId);
        itemRepository.flush();
        items.stream().sorted(java.util.Comparator.comparingInt(MediaCollectionChangedV2.Item::position))
                .limit(5)
                .forEach(item -> itemRepository.save(new PlaceMediaProjectionItem(
                        placeId,
                        item.mediaId(),
                        item.position(),
                        item.mediaId().equals(coverMediaId),
                        item.source(),
                        item.providerAssetKey(),
                        item.thumbnailUrl(),
                        item.cardUrl(),
                        item.detailUrl(),
                        item.sourceName(),
                        item.sourceLink(),
                        occurredAt)));
        collectionRepository.save(collection);
    }
}
