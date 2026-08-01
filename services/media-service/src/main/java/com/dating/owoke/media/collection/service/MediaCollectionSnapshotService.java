package com.dating.owoke.media.collection.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.domain.MediaAssetStatus;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.collection.domain.MediaCollectionItem;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.repository.MediaCollectionItemRepository;
import com.dating.owoke.media.collection.repository.MediaCollectionRepository;

@Service
public class MediaCollectionSnapshotService {

    private final MediaCollectionItemRepository itemRepository;
    private final MediaCollectionRepository collectionRepository;
    private final MediaAssetRepository assetRepository;

    public MediaCollectionSnapshotService(
            MediaCollectionItemRepository itemRepository,
            MediaCollectionRepository collectionRepository,
            MediaAssetRepository assetRepository) {
        this.itemRepository = itemRepository;
        this.collectionRepository = collectionRepository;
        this.assetRepository = assetRepository;
    }

    public MediaCollectionSnapshot readySnapshot(UUID placeId) {
        List<MediaCollectionItem> items = itemRepository.findActive(MediaOwnerType.PLACE, placeId);
        Map<UUID, MediaAsset> assets = assetRepository.findAllById(
                        items.stream().map(MediaCollectionItem::getMediaAssetId).toList())
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
        List<MediaCollectionItem> readyItems = items.stream()
                .filter(item -> {
                    MediaAsset asset = assets.get(item.getMediaAssetId());
                    return asset != null && asset.getStatus() == MediaAssetStatus.READY;
                })
                .toList();
        UUID cover = readyItems.stream()
                .filter(MediaCollectionItem::isCover)
                .map(MediaCollectionItem::getMediaAssetId)
                .findFirst()
                .orElse(null);
        long revision = collectionRepository.findById(placeId)
                .map(collection -> collection.getRevision())
                .orElse(0L);
        return new MediaCollectionSnapshot(
                placeId,
                cover,
                readyItems.stream().map(MediaCollectionItem::getMediaAssetId).toList(),
                revision);
    }
}
