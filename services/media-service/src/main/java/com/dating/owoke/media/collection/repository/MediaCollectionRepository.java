package com.dating.owoke.media.collection.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.media.collection.domain.MediaCollection;

import jakarta.persistence.LockModeType;

public interface MediaCollectionRepository extends JpaRepository<MediaCollection, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select collection from MediaCollection collection where collection.ownerId = :ownerId")
    Optional<MediaCollection> lockByOwnerId(UUID ownerId);
}
