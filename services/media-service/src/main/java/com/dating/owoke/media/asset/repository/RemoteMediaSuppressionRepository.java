package com.dating.owoke.media.asset.repository;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.media.asset.domain.RemoteMediaSuppression;
import com.dating.owoke.media.collection.domain.MediaOwnerType;

public interface RemoteMediaSuppressionRepository extends JpaRepository<RemoteMediaSuppression, UUID> {

    @Query("""
            select suppression.providerAssetKey from RemoteMediaSuppression suppression
            where suppression.ownerType = :ownerType and suppression.ownerId = :ownerId
              and suppression.provider = :provider
            """)
    Set<String> findKeys(MediaOwnerType ownerType, UUID ownerId, String provider);

    boolean existsByOwnerTypeAndOwnerIdAndProviderAndProviderAssetKey(
            MediaOwnerType ownerType, UUID ownerId, String provider, String providerAssetKey);
}
