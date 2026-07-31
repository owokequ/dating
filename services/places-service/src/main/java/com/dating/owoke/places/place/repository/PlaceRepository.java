package com.dating.owoke.places.place.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.dating.owoke.places.place.domain.Place;
import com.dating.owoke.places.place.domain.PlaceSource;

public interface PlaceRepository extends JpaRepository<Place, UUID>, JpaSpecificationExecutor<Place> {

    Optional<Place> findBySourceAndExternalId(PlaceSource source, String externalId);

    Optional<Place> findFirstByNormalizedNameAndNormalizedAddress(String normalizedName, String normalizedAddress);
}
