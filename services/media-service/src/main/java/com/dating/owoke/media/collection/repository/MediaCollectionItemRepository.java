package com.dating.owoke.media.collection.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.media.collection.domain.MediaCollectionItem;
import com.dating.owoke.media.collection.domain.MediaOwnerType;

import jakarta.persistence.LockModeType;

public interface MediaCollectionItemRepository extends JpaRepository<MediaCollectionItem, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item from MediaCollectionItem item
            where item.ownerType = :ownerType and item.ownerId = :ownerId and item.deletedAt is null
            order by item.position
            """)
    List<MediaCollectionItem> lockActive(MediaOwnerType ownerType, UUID ownerId);

    @Query("""
            select item from MediaCollectionItem item
            where item.ownerType = :ownerType and item.ownerId = :ownerId and item.deletedAt is null
            order by item.position
            """)
    List<MediaCollectionItem> findActive(MediaOwnerType ownerType, UUID ownerId);

    Optional<MediaCollectionItem> findByOwnerTypeAndOwnerIdAndMediaAssetIdAndDeletedAtIsNull(
            MediaOwnerType ownerType, UUID ownerId, UUID mediaAssetId);

    Optional<MediaCollectionItem> findByMediaAssetIdAndDeletedAtIsNull(UUID mediaAssetId);
}
