package com.dating.owoke.dating.placeprojection.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.placeprojection.domain.PlaceProjection;
import com.dating.owoke.dating.placeprojection.domain.PlaceProjectionStatus;
import com.dating.owoke.dating.placeprojection.repository.PlaceProjectionRepository;

@Service
public class PlaceProjectionService {

    private final PlaceProjectionRepository repository;
    private final Clock clock;

    public PlaceProjectionService(PlaceProjectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void upsert(UUID placeId, String name, String address, PlaceProjectionStatus status) {
        PlaceProjection place = repository.findById(placeId).orElse(null);
        if (place == null) {
            place = new PlaceProjection(placeId, name, address, status, clock.instant());
        } else {
            place.update(name, address, status, clock.instant());
        }
        repository.save(place);
    }
}
