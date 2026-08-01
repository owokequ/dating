package com.dating.owoke.media.processing.service;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.asset.repository.MediaVariantRepository;
import com.dating.owoke.media.storage.port.ObjectStorage;

@Component
@ConditionalOnProperty(prefix = "owoke.media.purge", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MediaPurgeService {

    private final MediaAssetRepository assetRepository;
    private final MediaVariantRepository variantRepository;
    private final ObjectStorage storage;
    private final Clock clock;

    public MediaPurgeService(
            MediaAssetRepository assetRepository,
            MediaVariantRepository variantRepository,
            ObjectStorage storage,
            Clock clock) {
        this.assetRepository = assetRepository;
        this.variantRepository = variantRepository;
        this.storage = storage;
        this.clock = clock;
    }

    @Scheduled(cron = "${owoke.media.purge.cron:0 15 3 * * *}")
    @Transactional
    public void purgeDueObjects() {
        for (MediaAsset asset : assetRepository.lockPurgeable(clock.instant(), PageRequest.of(0, 50))) {
            variantRepository.findAllByMediaAssetId(asset.getId())
                    .forEach(variant -> storage.delete(variant.getObjectKey()));
            storage.delete(com.dating.owoke.media.asset.service.MediaUploadService.sourceKey(asset.getId()));
            asset.markPurged(clock.instant());
        }
    }
}
