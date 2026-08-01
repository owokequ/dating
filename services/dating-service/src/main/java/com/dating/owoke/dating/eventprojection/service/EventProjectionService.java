package com.dating.owoke.dating.eventprojection.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.dating.eventprojection.domain.EventProjection;
import com.dating.owoke.dating.eventprojection.domain.EventProjectionStatus;
import com.dating.owoke.dating.eventprojection.domain.OccurrenceProjectionStatus;
import com.dating.owoke.dating.eventprojection.messaging.domain.EventChangedV1;
import com.dating.owoke.dating.eventprojection.repository.EventProjectionRepository;

@Service
public class EventProjectionService {
    private final EventProjectionRepository repository;
    public EventProjectionService(EventProjectionRepository repository) { this.repository = repository; }

    public void upsert(EventChangedV1 payload, Instant occurredAt) {
        EventProjection event = repository.findById(payload.eventId()).orElseGet(() -> new EventProjection(payload.eventId()));
        var venue = payload.venue();
        event.replace(payload.title(), payload.description(), payload.priceText(), payload.sourcePageUrl(),
                venue == null ? null : venue.name(), venue == null ? null : venue.address(),
                venue == null ? null : venue.latitude(), venue == null ? null : venue.longitude(), payload.localPlaceId(),
                EventProjectionStatus.valueOf(payload.status()), payload.occurrences().stream()
                        .map(item -> new EventProjection.OccurrenceData(item.occurrenceId(), item.startsAt(), item.endsAt(),
                                item.continuous(), OccurrenceProjectionStatus.valueOf(item.status())))
                        .toList(), occurredAt);
        repository.save(event);
    }

    public void updateMedia(UUID eventId, UUID coverMediaId, long collectionVersion) {
        repository.findById(eventId).ifPresent(event -> event.updateMedia(coverMediaId, collectionVersion));
    }
}
