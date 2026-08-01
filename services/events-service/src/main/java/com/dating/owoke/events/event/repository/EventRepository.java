package com.dating.owoke.events.event.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.dating.owoke.events.event.domain.CatalogEvent;

public interface EventRepository extends JpaRepository<CatalogEvent, UUID>, JpaSpecificationExecutor<CatalogEvent> {
    @EntityGraph(attributePaths = {"occurrences", "images"})
    Optional<CatalogEvent> findDetailedById(UUID id);
    Optional<CatalogEvent> findBySourceAndExternalId(String source, String externalId);
}
