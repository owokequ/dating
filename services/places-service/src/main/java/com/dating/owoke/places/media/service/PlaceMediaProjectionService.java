package com.dating.owoke.places.media.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.places.media.domain.PlaceMediaCollection;
import com.dating.owoke.places.media.domain.PlaceMediaProjectionItem;
import com.dating.owoke.places.media.repository.PlaceMediaCollectionRepository;
import com.dating.owoke.places.media.repository.PlaceMediaProjectionItemRepository;

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
            itemRepository.save(new PlaceMediaProjectionItem(
                    placeId, mediaId, position, mediaId.equals(coverMediaId), occurredAt));
        }
        collectionRepository.save(collection);
    }
}
