package com.dating.owoke.media.asset.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.media.asset.domain.MediaAssetStatus;
import com.dating.owoke.media.asset.domain.MediaVariant;
import com.dating.owoke.media.asset.domain.MediaVariantName;
import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.asset.repository.MediaVariantRepository;
import com.dating.owoke.media.shared.exception.ResourceNotFoundException;
import com.dating.owoke.media.storage.port.ObjectStorage;
import com.dating.owoke.media.storage.port.StoredObject;

@Service
public class MediaContentService {

    private final MediaAssetRepository assetRepository;
    private final MediaVariantRepository variantRepository;
    private final ObjectStorage storage;

    public MediaContentService(
            MediaAssetRepository assetRepository,
            MediaVariantRepository variantRepository,
            ObjectStorage storage) {
        this.assetRepository = assetRepository;
        this.variantRepository = variantRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public MediaContent get(UUID mediaId, MediaVariantName variantName) {
        assetRepository.findByIdAndStatus(mediaId, MediaAssetStatus.READY)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset is not available"));
        MediaVariant variant = variantRepository.findByMediaAssetIdAndVariant(mediaId, variantName)
                .orElseThrow(() -> new ResourceNotFoundException("Media variant is not available"));
        StoredObject object = storage.get(variant.getObjectKey());
        return new MediaContent(object.content(), variant.getContentType(), variant.getSha256());
    }
}
