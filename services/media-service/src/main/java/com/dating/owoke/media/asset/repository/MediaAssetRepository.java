package com.dating.owoke.media.asset.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.media.asset.domain.MediaAsset;
import com.dating.owoke.media.asset.domain.MediaAssetStatus;
import com.dating.owoke.media.collection.domain.MediaOwnerType;

import jakarta.persistence.LockModeType;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    Optional<MediaAsset> findByIdAndStatus(UUID id, MediaAssetStatus status);

    Optional<MediaAsset> findByOwnerTypeAndOwnerIdAndProviderAndProviderAssetKey(
            MediaOwnerType ownerType, UUID ownerId, String provider, String providerAssetKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select asset from MediaAsset asset where asset.id = :id")
    Optional<MediaAsset> lockById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select asset from MediaAsset asset where asset.status = 'UPLOADED' order by asset.createdAt")
    List<MediaAsset> lockUploaded(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select asset from MediaAsset asset
            where asset.status = 'DELETED' and asset.purgeAfter <= :now and asset.purgedAt is null
            order by asset.purgeAfter
            """)
    List<MediaAsset> lockPurgeable(Instant now, Pageable pageable);
}
