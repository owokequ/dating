package com.dating.owoke.media.collection.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.media.collection.domain.EventProjection;
import com.dating.owoke.media.collection.domain.EventProjectionStatus;
import com.dating.owoke.media.collection.repository.EventProjectionRepository;

@Service
public class EventProjectionService {

    private final EventProjectionRepository repository;

    public EventProjectionService(EventProjectionRepository repository) {
        this.repository = repository;
    }

    public void upsert(UUID eventId, EventProjectionStatus status, Instant occurredAt) {
        EventProjection projection = repository.findById(eventId)
                .orElseGet(() -> new EventProjection(eventId, status, occurredAt));
        projection.update(status, occurredAt);
        repository.save(projection);
    }
}
