package com.dating.owoke.media.processing.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dating.owoke.media.asset.service.MediaUploadService;
import com.dating.owoke.media.storage.port.ObjectStorage;

@Component
@ConditionalOnProperty(prefix = "owoke.media.processing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MediaProcessingCoordinator {

    private static final Logger log = LoggerFactory.getLogger(MediaProcessingCoordinator.class);
    private static final int BATCH_SIZE = 4;

    private final MediaProcessingLifecycleService lifecycle;
    private final ImageProcessor imageProcessor;
    private final ObjectStorage storage;

    public MediaProcessingCoordinator(
            MediaProcessingLifecycleService lifecycle,
            ImageProcessor imageProcessor,
            ObjectStorage storage) {
        this.lifecycle = lifecycle;
        this.imageProcessor = imageProcessor;
        this.storage = storage;
    }

    @Scheduled(fixedDelayString = "${owoke.media.processing.fixed-delay:500}")
    public void processPending() {
        for (int index = 0; index < BATCH_SIZE; index++) {
            UUID mediaId = lifecycle.claimNext().orElse(null);
            if (mediaId == null) {
                return;
            }
            process(mediaId);
        }
    }

    private void process(UUID mediaId) {
        List<String> uploadedKeys = new ArrayList<>();
        try {
            byte[] source = storage.get(MediaUploadService.sourceKey(mediaId)).content();
            ProcessedImage image = imageProcessor.process(source);
            for (ProcessedVariant variant : image.variants()) {
                String key = MediaProcessingLifecycleService.variantKey(mediaId, variant.name().name());
                storage.put(key, variant.content(), variant.contentType());
                uploadedKeys.add(key);
            }
            lifecycle.complete(mediaId, image);
        } catch (Exception exception) {
            uploadedKeys.forEach(key -> safelyDelete(key));
            lifecycle.fail(mediaId);
            log.warn("Media processing failed for {}: {}", mediaId, exception.getMessage());
            return;
        }
        safelyDelete(MediaUploadService.sourceKey(mediaId));
    }

    private void safelyDelete(String key) {
        try {
            storage.delete(key);
        } catch (Exception exception) {
            log.warn("Cannot clean incomplete media object {}", key);
        }
    }
}
