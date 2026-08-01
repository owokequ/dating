package com.dating.owoke.places.media.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.places.media.domain.PlaceMediaProjectionItem;

public interface PlaceMediaProjectionItemRepository extends JpaRepository<PlaceMediaProjectionItem, UUID> {

    List<PlaceMediaProjectionItem> findByPlaceIdInOrderByPlaceIdAscPositionAsc(Collection<UUID> placeIds);

    void deleteByPlaceId(UUID placeId);

    boolean existsByPlaceIdAndCoverTrue(UUID placeId);
}
