package com.dating.owoke.media.collection.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.media.collection.domain.MediaCollection;
import com.dating.owoke.media.collection.domain.MediaCollectionId;
import com.dating.owoke.media.collection.domain.MediaOwnerType;

import jakarta.persistence.LockModeType;

public interface MediaCollectionRepository extends JpaRepository<MediaCollection, MediaCollectionId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select collection from MediaCollection collection where collection.id.ownerType = :ownerType and collection.id.ownerId = :ownerId")
    Optional<MediaCollection> lockByOwner(MediaOwnerType ownerType, UUID ownerId);
}
