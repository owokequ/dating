package com.dating.owoke.media.asset.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.media.asset.domain.MediaVariant;
import com.dating.owoke.media.asset.domain.MediaVariantName;

public interface MediaVariantRepository extends JpaRepository<MediaVariant, UUID> {

    Optional<MediaVariant> findByMediaAssetIdAndVariant(UUID mediaAssetId, MediaVariantName variant);

    List<MediaVariant> findAllByMediaAssetId(UUID mediaAssetId);

    void deleteAllByMediaAssetId(UUID mediaAssetId);
}
