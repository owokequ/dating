package com.dating.owoke.places.media.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.places.media.domain.PlaceMediaCollection;

public interface PlaceMediaCollectionRepository extends JpaRepository<PlaceMediaCollection, UUID> {
}
