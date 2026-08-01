package com.dating.owoke.media.asset.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.media.asset.domain.MediaAssetStatus;
import com.dating.owoke.media.asset.domain.MediaAssetSource;
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
    private final RemoteImageFetcher remoteImageFetcher;

    public MediaContentService(
            MediaAssetRepository assetRepository,
            MediaVariantRepository variantRepository,
            ObjectStorage storage,
            RemoteImageFetcher remoteImageFetcher) {
        this.assetRepository = assetRepository;
        this.variantRepository = variantRepository;
        this.storage = storage;
        this.remoteImageFetcher = remoteImageFetcher;
    }

    public MediaContent get(UUID mediaId, MediaVariantName variantName) {
        var asset = assetRepository.findByIdAndStatus(mediaId, MediaAssetStatus.READY)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset is not available"));
        if (asset.getSource() == MediaAssetSource.REMOTE_URL) {
            if (variantName != MediaVariantName.TELEGRAM) {
                throw new ResourceNotFoundException("Remote media is exposed directly for web variants");
            }
            var fetched = remoteImageFetcher.fetch(asset.getRemoteUrl());
            return new MediaContent(fetched.content(), fetched.contentType(), digest(fetched.content()));
        }
        MediaVariant variant = variantRepository.findByMediaAssetIdAndVariant(mediaId, variantName)
                .orElseThrow(() -> new ResourceNotFoundException("Media variant is not available"));
        StoredObject object = storage.get(variant.getObjectKey());
        return new MediaContent(object.content(), variant.getContentType(), variant.getSha256());
    }

    private String digest(byte[] content) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(content));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
