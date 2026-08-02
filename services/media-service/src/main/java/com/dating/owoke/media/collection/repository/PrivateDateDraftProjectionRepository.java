package com.dating.owoke.media.collection.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.media.collection.domain.PrivateDateDraftProjection;

public interface PrivateDateDraftProjectionRepository extends JpaRepository<PrivateDateDraftProjection, UUID> {
}
