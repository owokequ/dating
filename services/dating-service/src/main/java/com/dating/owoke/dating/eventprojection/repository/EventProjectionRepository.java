package com.dating.owoke.dating.eventprojection.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.dating.eventprojection.domain.EventProjection;

public interface EventProjectionRepository extends JpaRepository<EventProjection, UUID> {
}
