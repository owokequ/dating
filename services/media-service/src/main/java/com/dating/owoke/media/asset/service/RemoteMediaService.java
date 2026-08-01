package com.dating.owoke.media.asset.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.domain.MediaAssetSource;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.asset.repository.RemoteMediaSuppressionRepository;
import com.dating.owoke.media.collection.domain.MediaCollection;
import com.dating.owoke.media.collection.domain.MediaCollectionItem;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.repository.MediaCollectionItemRepository;
import com.dating.owoke.media.collection.repository.MediaCollectionRepository;
import com.dating.owoke.media.collection.service.MediaCollectionEventService;
import com.dating.owoke.media.collection.service.MediaCollectionSnapshotService;

import jakarta.persistence.EntityManager;

@Service
public class RemoteMediaService {

    public static final String KUDAGO = "KUDAGO";
    private static final int MAX_IMAGES = 5;
    private static final int TEMPORARY_POSITION_OFFSET = 100;

    private final MediaAssetRepository assetRepository;
    private final RemoteMediaSuppressionRepository suppressionRepository;
    private final MediaCollectionRepository collectionRepository;
    private final MediaCollectionItemRepository itemRepository;
    private final MediaCollectionSnapshotService snapshotService;
    private final MediaCollectionEventService eventService;
    private final KudaGoUrlPolicy urlPolicy;
    private final EntityManager entityManager;
    private final Clock clock;

    public RemoteMediaService(
            MediaAssetRepository assetRepository,
            RemoteMediaSuppressionRepository suppressionRepository,
            MediaCollectionRepository collectionRepository,
            MediaCollectionItemRepository itemRepository,
            MediaCollectionSnapshotService snapshotService,
            MediaCollectionEventService eventService,
            KudaGoUrlPolicy urlPolicy,
            EntityManager entityManager,
            Clock clock) {
        this.assetRepository = assetRepository;
        this.suppressionRepository = suppressionRepository;
        this.collectionRepository = collectionRepository;
        this.itemRepository = itemRepository;
        this.snapshotService = snapshotService;
        this.eventService = eventService;
        this.urlPolicy = urlPolicy;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public void synchronize(MediaOwnerType ownerType, UUID ownerId, List<RemoteImageDescriptor> images) {
        List<RemoteImageDescriptor> normalized = normalize(images);
        Set<String> suppressed = suppressionRepository.findKeys(ownerType, ownerId, KUDAGO);
        List<MediaCollectionItem> active = itemRepository.lockActive(ownerType, ownerId);
        Map<UUID, MediaAsset> assets = assetRepository.findAllById(
                        active.stream().map(MediaCollectionItem::getMediaAssetId).toList())
                .stream().collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
        int ownCount = (int) active.stream()
                .map(item -> assets.get(item.getMediaAssetId()))
                .filter(asset -> asset != null && asset.getSource() == MediaAssetSource.UPLOAD)
                .count();
        List<RemoteImageDescriptor> selected = normalized.stream()
                .filter(image -> !suppressed.contains(image.providerAssetKey()))
                .limit(Math.max(0, MAX_IMAGES - ownCount))
                .toList();
        Set<String> selectedKeys = selected.stream()
                .map(RemoteImageDescriptor::providerAssetKey).collect(Collectors.toSet());

        boolean changed = removeMissingRemote(active, assets, selectedKeys);
        List<MediaCollectionItem> current = active.stream()
                .filter(item -> !isDeletedRemote(item, assets, selectedKeys))
                .collect(Collectors.toCollection(ArrayList::new));
        Instant now = clock.instant();
        for (RemoteImageDescriptor image : selected) {
            MediaAsset asset = assetRepository
                    .findByOwnerTypeAndOwnerIdAndProviderAndProviderAssetKey(
                            ownerType, ownerId, KUDAGO, image.providerAssetKey())
                    .orElse(null);
            if (asset == null) {
                asset = assetRepository.save(MediaAsset.remote(
                        deterministicId(ownerType, ownerId, image.providerAssetKey()),
                        ownerType,
                        ownerId,
                        KUDAGO,
                        image.providerAssetKey(),
                        image.remoteUrl(),
                        image.sourceName(),
                        image.sourceLink(),
                        now));
                current.add(itemRepository.save(new MediaCollectionItem(
                        ownerType, ownerId, asset.getId(), current.size(), false, now)));
                assets.put(asset.getId(), asset);
                changed = true;
            } else {
                UUID existingAssetId = asset.getId();
                boolean attached = current.stream()
                        .anyMatch(item -> item.getMediaAssetId().equals(existingAssetId));
                if (!attached) {
                    asset.restoreRemote(image.remoteUrl(), image.sourceName(), image.sourceLink(), now);
                    current.add(itemRepository.save(new MediaCollectionItem(
                            ownerType, ownerId, asset.getId(), current.size(), false, now)));
                    assets.put(asset.getId(), asset);
                    changed = true;
                } else {
                    changed |= asset.refreshRemote(image.remoteUrl(), image.sourceName(), image.sourceLink());
                }
            }
        }
        if (!changed) {
            return;
        }
        MediaCollection collection = collectionRepository.lockByOwner(ownerType, ownerId)
                .orElseGet(() -> new MediaCollection(ownerType, ownerId, now));
        normalizeOrderAndCover(current, assets);
        collection.changed(now);
        collectionRepository.save(collection);
        entityManager.flush();
        eventService.collectionChanged(snapshotService.readySnapshot(ownerType, ownerId));
    }

    private boolean removeMissingRemote(
            List<MediaCollectionItem> items,
            Map<UUID, MediaAsset> assets,
            Set<String> selectedKeys) {
        boolean changed = false;
        Instant now = clock.instant();
        for (MediaCollectionItem item : items) {
            MediaAsset asset = assets.get(item.getMediaAssetId());
            if (asset != null && asset.getSource() == MediaAssetSource.REMOTE_URL
                    && !selectedKeys.contains(asset.getProviderAssetKey())) {
                item.delete(now);
                asset.suppressRemote(now);
                changed = true;
            }
        }
        return changed;
    }

    private boolean isDeletedRemote(
            MediaCollectionItem item,
            Map<UUID, MediaAsset> assets,
            Set<String> selectedKeys) {
        MediaAsset asset = assets.get(item.getMediaAssetId());
        return asset != null && asset.getSource() == MediaAssetSource.REMOTE_URL
                && !selectedKeys.contains(asset.getProviderAssetKey());
    }

    private void normalizeOrderAndCover(List<MediaCollectionItem> items, Map<UUID, MediaAsset> assets) {
        UUID existingCover = items.stream().filter(MediaCollectionItem::isCover)
                .map(MediaCollectionItem::getMediaAssetId).findFirst().orElse(null);
        UUID firstOwn = items.stream()
                .map(MediaCollectionItem::getMediaAssetId)
                .filter(id -> assets.get(id) != null && assets.get(id).getSource() == MediaAssetSource.UPLOAD)
                .findFirst().orElse(null);
        UUID cover = firstOwn != null ? firstOwn
                : existingCover != null ? existingCover
                : items.stream().map(MediaCollectionItem::getMediaAssetId).findFirst().orElse(null);
        for (int index = 0; index < items.size(); index++) {
            items.get(index).reorder(TEMPORARY_POSITION_OFFSET + index, false);
        }
        entityManager.flush();
        for (int index = 0; index < items.size(); index++) {
            MediaCollectionItem item = items.get(index);
            item.reorder(index, item.getMediaAssetId().equals(cover));
        }
    }

    private List<RemoteImageDescriptor> normalize(List<RemoteImageDescriptor> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        Set<String> keys = new HashSet<>();
        List<RemoteImageDescriptor> result = new ArrayList<>();
        for (RemoteImageDescriptor image : images) {
            if (image == null || image.providerAssetKey() == null || image.providerAssetKey().isBlank()) {
                throw new IllegalArgumentException("Remote image provider key must not be blank");
            }
            if (!keys.add(image.providerAssetKey())) {
                continue;
            }
            result.add(new RemoteImageDescriptor(
                    image.providerAssetKey(),
                    urlPolicy.imageUrl(image.remoteUrl()),
                    image.sourceName() == null ? "KudaGo" : image.sourceName(),
                    urlPolicy.sourceLink(image.sourceLink())));
            if (result.size() == MAX_IMAGES) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private UUID deterministicId(MediaOwnerType ownerType, UUID ownerId, String providerAssetKey) {
        return UUID.nameUUIDFromBytes(
                (ownerType + ":" + ownerId + ":" + KUDAGO + ":" + providerAssetKey)
                        .getBytes(StandardCharsets.UTF_8));
    }
}
