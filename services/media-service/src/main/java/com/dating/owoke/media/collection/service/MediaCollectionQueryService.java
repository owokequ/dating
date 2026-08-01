package com.dating.owoke.media.collection.service;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.domain.MediaAssetStatus;
import com.dating.owoke.media.asset.dto.MediaItemResponse;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.collection.domain.MediaCollectionItem;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.dto.MediaCollectionResponse;
import com.dating.owoke.media.collection.repository.MediaCollectionItemRepository;
import com.dating.owoke.media.collection.repository.MediaCollectionRepository;

@Service
public class MediaCollectionQueryService {

    private final MediaCollectionItemRepository itemRepository;
    private final MediaCollectionRepository collectionRepository;
    private final MediaAssetRepository assetRepository;

    public MediaCollectionQueryService(
            MediaCollectionItemRepository itemRepository,
            MediaCollectionRepository collectionRepository,
            MediaAssetRepository assetRepository) {
        this.itemRepository = itemRepository;
        this.collectionRepository = collectionRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional(readOnly = true)
    public MediaCollectionResponse get(UUID placeId) {
        var items = itemRepository.findActive(MediaOwnerType.PLACE, placeId);
        Map<UUID, MediaAsset> assets = assetRepository.findAllById(
                        items.stream().map(MediaCollectionItem::getMediaAssetId).toList())
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
        var responses = items.stream().map(item -> response(item, assets.get(item.getMediaAssetId()))).toList();
        UUID cover = items.stream().filter(MediaCollectionItem::isCover)
                .map(MediaCollectionItem::getMediaAssetId).findFirst().orElse(null);
        long revision = collectionRepository.findById(placeId)
                .map(collection -> collection.getRevision()).orElse(0L);
        return new MediaCollectionResponse(placeId, cover, revision, responses);
    }

    private MediaItemResponse response(MediaCollectionItem item, MediaAsset asset) {
        boolean ready = asset != null && asset.getStatus() == MediaAssetStatus.READY;
        UUID mediaId = item.getMediaAssetId();
        String base = "/api/v1/media/assets/" + mediaId + "/content?variant=";
        String version = ready ? "&v=" + asset.getSha256() : "";
        return new MediaItemResponse(
                mediaId,
                asset == null ? "FAILED" : asset.getStatus().name(),
                item.getPosition(),
                item.isCover(),
                ready ? base + "THUMBNAIL" + version : null,
                ready ? base + "CARD" + version : null,
                ready ? base + "DETAIL" + version : null);
    }
}
