package com.dating.owoke.media.collection.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.media.collection.domain.PlaceProjection;
import com.dating.owoke.media.collection.domain.PlaceProjectionStatus;
import com.dating.owoke.media.collection.repository.PlaceProjectionRepository;

@Service
public class PlaceProjectionService {

    private final PlaceProjectionRepository repository;

    public PlaceProjectionService(PlaceProjectionRepository repository) {
        this.repository = repository;
    }

    public void upsert(UUID placeId, PlaceProjectionStatus status, Instant occurredAt) {
        PlaceProjection projection = repository.findById(placeId)
                .orElseGet(() -> new PlaceProjection(placeId, status, occurredAt));
        projection.update(status, occurredAt);
        repository.save(projection);
    }
}
