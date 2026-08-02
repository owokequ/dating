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
import com.dating.owoke.media.collection.repository.EventProjectionRepository;
import com.dating.owoke.media.collection.repository.PrivateDateDraftProjectionRepository;
import com.dating.owoke.media.processing.configuration.MediaProcessingProperties;
import com.dating.owoke.media.processing.exception.InvalidImageException;
import com.dating.owoke.media.processing.service.ImageFormatDetector;
import com.dating.owoke.media.shared.exception.BusinessConflictException;
import com.dating.owoke.media.shared.exception.ResourceNotFoundException;
import com.dating.owoke.media.storage.port.ObjectStorage;

import jakarta.persistence.EntityManager;

@Service
public class MediaUploadService {

    private static final int MAX_IMAGES = 5;

    private final MediaAssetRepository assetRepository;
    private final MediaCollectionRepository collectionRepository;
    private final MediaCollectionItemRepository itemRepository;
    private final PlaceProjectionRepository placeRepository;
    private final EventProjectionRepository eventRepository;
    private final PrivateDateDraftProjectionRepository privateDraftRepository;
    private final ObjectStorage storage;
    private final MediaProcessingProperties properties;
    private final Clock clock;
    private final EntityManager entityManager;

    public MediaUploadService(
            MediaAssetRepository assetRepository,
            MediaCollectionRepository collectionRepository,
            MediaCollectionItemRepository itemRepository,
            PlaceProjectionRepository placeRepository,
            EventProjectionRepository eventRepository,
            PrivateDateDraftProjectionRepository privateDraftRepository,
            ObjectStorage storage,
            MediaProcessingProperties properties,
            EntityManager entityManager,
            Clock clock) {
        this.assetRepository = assetRepository;
        this.collectionRepository = collectionRepository;
        this.itemRepository = itemRepository;
        this.placeRepository = placeRepository;
        this.eventRepository = eventRepository;
        this.privateDraftRepository = privateDraftRepository;
        this.storage = storage;
        this.properties = properties;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public MediaUploadResponse upload(UUID placeId, UUID uploadedBy, MultipartFile file) {
        return upload(MediaOwnerType.PLACE, placeId, uploadedBy, file);
    }

    @Transactional
    public MediaUploadResponse uploadEvent(UUID eventId, UUID uploadedBy, MultipartFile file) {
        return upload(MediaOwnerType.EVENT, eventId, uploadedBy, file);
    }

    @Transactional
    public MediaUploadResponse uploadPrivateDateProposal(UUID proposalId, UUID uploadedBy, MultipartFile file) {
        return upload(MediaOwnerType.DATE_PROPOSAL, proposalId, uploadedBy, file);
    }

    private MediaUploadResponse upload(
            MediaOwnerType ownerType, UUID ownerId, UUID uploadedBy, MultipartFile file) {
        validateOwner(ownerType, ownerId, uploadedBy);

        List<MediaCollectionItem> items = itemRepository.lockActive(ownerType, ownerId);
        if (items.size() >= MAX_IMAGES) {
            throw new BusinessConflictException("A media collection can contain at most five images");
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
        asset.attachToOwner(ownerType, ownerId);
        boolean replaceRemoteCover = items.stream()
                .filter(MediaCollectionItem::isCover)
                .map(item -> assetRepository.findById(item.getMediaAssetId()).orElse(null))
                .anyMatch(existing -> existing != null && existing.getSource() == MediaAssetSource.REMOTE_URL);
        if (replaceRemoteCover) {
            items.forEach(item -> item.reorder(item.getPosition(), false));
            entityManager.flush();
        }
        itemRepository.save(new MediaCollectionItem(
                ownerType,
                ownerId,
                mediaId,
                items.size(),
                items.isEmpty() || replaceRemoteCover,
                clock.instant()));
        MediaCollection collection = collectionRepository.lockByOwner(ownerType, ownerId)
                .orElseGet(() -> new MediaCollection(ownerType, ownerId, clock.instant()));
        collection.changed(clock.instant());
        collectionRepository.save(collection);
        return new MediaUploadResponse(asset.getId(), asset.getStatus().name());
    }

    private void validateOwner(MediaOwnerType ownerType, UUID ownerId, UUID uploadedBy) {
        if (ownerType == MediaOwnerType.PLACE) {
            PlaceProjection place = placeRepository.lockById(ownerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Place is not available in media projection yet"));
            if (!place.acceptsUploads()) {
                throw new BusinessConflictException("Images cannot be added to an archived place");
            }
            return;
        }
        if (ownerType == MediaOwnerType.DATE_PROPOSAL) {
            var draft = privateDraftRepository.findById(ownerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Private date draft is not available in media projection yet"));
            if (!draft.canUpload(uploadedBy, clock.instant())) {
                throw new BusinessConflictException("Images can only be added by the private date draft author before sending");
            }
            return;
        }
        var event = eventRepository.lockById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Event is not available in media projection yet"));
        if (!event.acceptsUploads()) {
            throw new BusinessConflictException("Images cannot be added to a hidden or archived event");
        }
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
