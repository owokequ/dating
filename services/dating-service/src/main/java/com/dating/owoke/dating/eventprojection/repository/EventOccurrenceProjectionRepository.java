package com.dating.owoke.dating.eventprojection.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.dating.eventprojection.domain.EventOccurrenceProjection;

public interface EventOccurrenceProjectionRepository extends JpaRepository<EventOccurrenceProjection, UUID> {
    @EntityGraph(attributePaths = "event")
    Optional<EventOccurrenceProjection> findDetailedById(UUID id);
}
