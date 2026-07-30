package com.dating.owoke.dating.placeprojection.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dating.owoke.dating.placeprojection.domain.PlaceProjection;

public interface PlaceProjectionRepository extends JpaRepository<PlaceProjection, UUID> {
}
