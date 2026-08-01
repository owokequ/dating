package com.dating.owoke.media.collection.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.domain.MediaAssetStatus;
import com.dating.owoke.media.asset.domain.MediaAssetSource;
import com.dating.owoke.media.asset.dto.MediaItemResponse;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.collection.domain.MediaCollectionItem;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.repository.MediaCollectionItemRepository;
import com.dating.owoke.media.collection.repository.MediaCollectionRepository;
import com.dating.owoke.media.collection.domain.MediaCollectionId;

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

    public MediaCollectionSnapshot readySnapshot(MediaOwnerType ownerType, UUID ownerId) {
        List<MediaCollectionItem> items = itemRepository.findActive(ownerType, ownerId);
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
        long revision = collectionRepository.findById(new MediaCollectionId(ownerType, ownerId))
                .map(collection -> collection.getRevision())
                .orElse(0L);
        return new MediaCollectionSnapshot(
                ownerType,
                ownerId,
                cover,
                readyItems.stream().map(MediaCollectionItem::getMediaAssetId).toList(),
                readyItems.stream().map(item -> response(item, assets.get(item.getMediaAssetId()))).toList(),
                revision);
    }

    private MediaItemResponse response(MediaCollectionItem item, MediaAsset asset) {
        boolean remote = asset.getSource() == MediaAssetSource.REMOTE_URL;
        String base = "/api/v1/media/assets/" + asset.getId() + "/content?variant=";
        String version = asset.getSha256() == null ? "" : "&v=" + asset.getSha256();
        String url = remote ? asset.getRemoteUrl() : null;
        return new MediaItemResponse(
                asset.getId(),
                asset.getSource().name(),
                asset.getProviderAssetKey(),
                asset.getStatus().name(),
                item.getPosition(),
                item.isCover(),
                remote ? url : base + "THUMBNAIL" + version,
                remote ? url : base + "CARD" + version,
                remote ? url : base + "DETAIL" + version,
                remote ? asset.getSourceName() : null,
                remote ? asset.getSourceLink() : null);
    }
}
