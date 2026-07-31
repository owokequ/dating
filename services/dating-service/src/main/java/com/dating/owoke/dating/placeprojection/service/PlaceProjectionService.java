package com.dating.owoke.dating.placeprojection.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.placeprojection.domain.PlaceProjection;
import com.dating.owoke.dating.placeprojection.domain.PlaceProjectionStatus;
import com.dating.owoke.dating.placeprojection.repository.PlaceProjectionRepository;

@Service
public class PlaceProjectionService {

    private final PlaceProjectionRepository repository;
    public PlaceProjectionService(PlaceProjectionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void upsert(
            UUID placeId,
            String name,
            String address,
            PlaceProjectionStatus status,
            Instant eventOccurredAt) {
        PlaceProjection place = repository.findById(placeId).orElse(null);
        if (place == null) {
            repository.save(new PlaceProjection(placeId, name, address, status, eventOccurredAt));
            return;
        }
        if (place.updateIfNewer(name, address, status, eventOccurredAt)) {
            repository.save(place);
        }
    }
}
