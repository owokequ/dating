package com.dating.owoke.media.processing.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.domain.MediaVariant;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.asset.repository.MediaVariantRepository;
import com.dating.owoke.media.collection.domain.MediaCollection;
import com.dating.owoke.media.collection.domain.MediaCollectionItem;
import com.dating.owoke.media.collection.repository.MediaCollectionItemRepository;
import com.dating.owoke.media.collection.repository.MediaCollectionRepository;
import com.dating.owoke.media.collection.service.MediaCollectionEventService;
import com.dating.owoke.media.collection.service.MediaCollectionSnapshotService;

@Service
public class MediaProcessingLifecycleService {

    private final MediaAssetRepository assetRepository;
    private final MediaVariantRepository variantRepository;
    private final MediaCollectionItemRepository itemRepository;
    private final MediaCollectionRepository collectionRepository;
    private final MediaCollectionSnapshotService snapshotService;
    private final MediaCollectionEventService eventService;
    private final Clock clock;

    public MediaProcessingLifecycleService(
            MediaAssetRepository assetRepository,
            MediaVariantRepository variantRepository,
            MediaCollectionItemRepository itemRepository,
            MediaCollectionRepository collectionRepository,
            MediaCollectionSnapshotService snapshotService,
            MediaCollectionEventService eventService,
            Clock clock) {
        this.assetRepository = assetRepository;
        this.variantRepository = variantRepository;
        this.itemRepository = itemRepository;
        this.collectionRepository = collectionRepository;
        this.snapshotService = snapshotService;
        this.eventService = eventService;
        this.clock = clock;
    }

    @Transactional
    public Optional<UUID> claimNext() {
        List<MediaAsset> assets = assetRepository.lockUploaded(PageRequest.of(0, 1));
        if (assets.isEmpty()) {
            return Optional.empty();
        }
        MediaAsset asset = assets.getFirst();
        asset.markProcessing();
        return Optional.of(asset.getId());
    }

    @Transactional
    public void complete(UUID mediaId, ProcessedImage image) {
        MediaAsset asset = assetRepository.lockById(mediaId)
                .orElseThrow(() -> new IllegalStateException("Processing media disappeared"));
        MediaCollectionItem item = itemRepository.findByMediaAssetIdAndDeletedAtIsNull(mediaId)
                .orElseThrow(() -> new IllegalStateException("Media collection item disappeared"));
        List<MediaVariant> variants = new ArrayList<>();
        for (ProcessedVariant processed : image.variants()) {
            variants.add(new MediaVariant(
                    mediaId,
                    processed.name(),
                    variantKey(mediaId, processed.name().name()),
                    processed.contentType(),
                    processed.width(),
                    processed.height(),
                    processed.content().length,
                    processed.sha256(),
                    clock.instant()));
        }
        variantRepository.saveAll(variants);
        asset.markReady(
                image.detectedContentType(),
                image.originalWidth(),
                image.originalHeight(),
                image.sourceSha256(),
                clock.instant());
        MediaCollection collection = collectionRepository.lockByOwnerId(item.getOwnerId())
                .orElseThrow(() -> new IllegalStateException("Media collection disappeared"));
        collection.changed(clock.instant());
        assetRepository.flush();
        eventService.assetReady(item.getOwnerId(), asset, variants);
        eventService.collectionChanged(snapshotService.readySnapshot(item.getOwnerId()));
    }

    @Transactional
    public void fail(UUID mediaId) {
        assetRepository.lockById(mediaId).ifPresent(MediaAsset::markFailed);
    }

    public static String variantKey(UUID mediaId, String variant) {
        return "variants/" + mediaId + "/" + variant.toLowerCase(java.util.Locale.ROOT) + ".jpg";
    }
}
