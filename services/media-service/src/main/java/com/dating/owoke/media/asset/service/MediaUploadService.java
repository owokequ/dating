package com.dating.owoke.media.asset.service;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.domain.MediaAssetSource;
import com.dating.owoke.media.asset.dto.MediaUploadResponse;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.collection.domain.MediaCollection;
import com.dating.owoke.media.collection.domain.MediaCollectionItem;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.domain.PlaceProjection;
import com.dating.owoke.media.collection.repository.MediaCollectionItemRepository;
import com.dating.owoke.media.collection.repository.MediaCollectionRepository;
import com.dating.owoke.media.collection.repository.PlaceProjectionRepository;
import com.dating.owoke.media.processing.configuration.MediaProcessingProperties;
import com.dating.owoke.media.processing.exception.InvalidImageException;
import com.dating.owoke.media.processing.service.ImageFormatDetector;
import com.dating.owoke.media.shared.exception.BusinessConflictException;
import com.dating.owoke.media.shared.exception.ResourceNotFoundException;
import com.dating.owoke.media.storage.port.ObjectStorage;

@Service
public class MediaUploadService {

    private static final int MAX_PLACE_IMAGES = 5;

    private final MediaAssetRepository assetRepository;
    private final MediaCollectionRepository collectionRepository;
    private final MediaCollectionItemRepository itemRepository;
    private final PlaceProjectionRepository placeRepository;
    private final ObjectStorage storage;
    private final MediaProcessingProperties properties;
    private final Clock clock;

    public MediaUploadService(
            MediaAssetRepository assetRepository,
            MediaCollectionRepository collectionRepository,
            MediaCollectionItemRepository itemRepository,
            PlaceProjectionRepository placeRepository,
            ObjectStorage storage,
            MediaProcessingProperties properties,
            Clock clock) {
        this.assetRepository = assetRepository;
        this.collectionRepository = collectionRepository;
        this.itemRepository = itemRepository;
        this.placeRepository = placeRepository;
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public MediaUploadResponse upload(UUID placeId, UUID uploadedBy, MultipartFile file) {
        PlaceProjection place = placeRepository.lockById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place is not available in media projection yet"));
        if (!place.isActive()) {
            throw new BusinessConflictException("Images cannot be added to an archived place");
        }

        List<MediaCollectionItem> items = itemRepository.lockActive(MediaOwnerType.PLACE, placeId);
        if (items.size() >= MAX_PLACE_IMAGES) {
            throw new BusinessConflictException("A place can contain at most five images");
        }

        byte[] content = read(file);
        String detectedType = ImageFormatDetector.detect(content);
        UUID mediaId = UUID.randomUUID();
        String sourceKey = sourceKey(mediaId);
        storage.put(sourceKey, content, detectedType);
        deleteSourceIfTransactionRollsBack(sourceKey);

        MediaAsset asset = assetRepository.save(new MediaAsset(
                mediaId,
                MediaAssetSource.UPLOAD,
                file.getOriginalFilename(),
                content.length,
                uploadedBy,
                clock.instant()));
        itemRepository.save(new MediaCollectionItem(
                placeId,
                mediaId,
                items.size(),
                items.isEmpty(),
                clock.instant()));
        MediaCollection collection = collectionRepository.lockByOwnerId(placeId)
                .orElseGet(() -> new MediaCollection(placeId, clock.instant()));
        collection.changed(clock.instant());
        collectionRepository.save(collection);
        return new MediaUploadResponse(asset.getId(), asset.getStatus().name());
    }

    private byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Image file must not be empty");
        }
        if (file.getSize() > properties.maximumSourceBytes()) {
            throw new InvalidImageException("Image is larger than the configured limit");
        }
        try {
            byte[] content = file.getBytes();
            if (content.length > properties.maximumSourceBytes()) {
                throw new InvalidImageException("Image is larger than the configured limit");
            }
            return content;
        } catch (IOException exception) {
            throw new InvalidImageException("Image upload cannot be read", exception);
        }
    }

    private void deleteSourceIfTransactionRollsBack(String sourceKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storage.delete(sourceKey);
                }
            }
        });
    }

    public static String sourceKey(UUID mediaId) {
        return "sources/" + mediaId;
    }
}
