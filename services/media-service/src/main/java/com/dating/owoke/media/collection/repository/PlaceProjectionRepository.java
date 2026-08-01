package com.dating.owoke.media.collection.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.dating.owoke.media.collection.domain.PlaceProjection;

import jakarta.persistence.LockModeType;

public interface PlaceProjectionRepository extends JpaRepository<PlaceProjection, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select place from PlaceProjection place where place.placeId = :placeId")
    java.util.Optional<PlaceProjection> lockById(UUID placeId);
}
