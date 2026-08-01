package com.dating.owoke.media.collection.service;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.domain.MediaAssetStatus;
import com.dating.owoke.media.asset.domain.MediaAssetSource;
import com.dating.owoke.media.asset.dto.MediaItemResponse;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.collection.domain.MediaCollectionItem;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.dto.MediaCollectionResponse;
import com.dating.owoke.media.collection.repository.MediaCollectionItemRepository;
import com.dating.owoke.media.collection.repository.MediaCollectionRepository;
import com.dating.owoke.media.collection.repository.PlaceProjectionRepository;
import com.dating.owoke.media.collection.repository.EventProjectionRepository;
import com.dating.owoke.media.collection.domain.MediaCollectionId;
import com.dating.owoke.media.shared.exception.ResourceNotFoundException;

@Service
public class MediaCollectionQueryService {

    private final MediaCollectionItemRepository itemRepository;
    private final MediaCollectionRepository collectionRepository;
    private final MediaAssetRepository assetRepository;
    private final PlaceProjectionRepository placeRepository;
    private final EventProjectionRepository eventRepository;

    public MediaCollectionQueryService(
            MediaCollectionItemRepository itemRepository,
            MediaCollectionRepository collectionRepository,
            MediaAssetRepository assetRepository,
            PlaceProjectionRepository placeRepository,
            EventProjectionRepository eventRepository) {
        this.itemRepository = itemRepository;
        this.collectionRepository = collectionRepository;
        this.assetRepository = assetRepository;
        this.placeRepository = placeRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public MediaCollectionResponse getPublic(UUID placeId) {
        var place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place media collection is not available"));
        if (!place.isActive()) {
            throw new ResourceNotFoundException("Place media collection is not available");
        }
        return assemble(MediaOwnerType.PLACE, placeId);
    }

    @Transactional(readOnly = true)
    public MediaCollectionResponse getAdmin(UUID placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new ResourceNotFoundException("Place media collection is not available");
        }
        return assemble(MediaOwnerType.PLACE, placeId);
    }

    @Transactional(readOnly = true)
    public MediaCollectionResponse getPublicEvent(UUID eventId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event media collection is not available"));
        if (!event.isActive()) {
            throw new ResourceNotFoundException("Event media collection is not available");
        }
        return assemble(MediaOwnerType.EVENT, eventId);
    }

    @Transactional(readOnly = true)
    public MediaCollectionResponse getAdminEvent(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event media collection is not available");
        }
        return assemble(MediaOwnerType.EVENT, eventId);
    }

    private MediaCollectionResponse assemble(MediaOwnerType ownerType, UUID ownerId) {
        var items = itemRepository.findActive(ownerType, ownerId);
        Map<UUID, MediaAsset> assets = assetRepository.findAllById(
                        items.stream().map(MediaCollectionItem::getMediaAssetId).toList())
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
        var responses = items.stream().map(item -> response(item, assets.get(item.getMediaAssetId()))).toList();
        UUID cover = items.stream().filter(MediaCollectionItem::isCover)
                .map(MediaCollectionItem::getMediaAssetId).findFirst().orElse(null);
        long revision = collectionRepository.findById(new MediaCollectionId(ownerType, ownerId))
                .map(collection -> collection.getRevision()).orElse(0L);
        return new MediaCollectionResponse(ownerType, ownerId, cover, revision, responses);
    }

    private MediaItemResponse response(MediaCollectionItem item, MediaAsset asset) {
        boolean ready = asset != null && asset.getStatus() == MediaAssetStatus.READY;
        UUID mediaId = item.getMediaAssetId();
        boolean remote = ready && asset.getSource() == MediaAssetSource.REMOTE_URL;
        String base = "/api/v1/media/assets/" + mediaId + "/content?variant=";
        String version = ready && asset.getSha256() != null ? "&v=" + asset.getSha256() : "";
        String remoteUrl = remote ? asset.getRemoteUrl() : null;
        return new MediaItemResponse(
                mediaId,
                asset == null ? "UNKNOWN" : asset.getSource().name(),
                asset == null ? null : asset.getProviderAssetKey(),
                asset == null ? "FAILED" : asset.getStatus().name(),
                item.getPosition(),
                item.isCover(),
                ready ? remote ? remoteUrl : base + "THUMBNAIL" + version : null,
                ready ? remote ? remoteUrl : base + "CARD" + version : null,
                ready ? remote ? remoteUrl : base + "DETAIL" + version : null,
                remote ? asset.getSourceName() : null,
                remote ? asset.getSourceLink() : null);
    }
}
