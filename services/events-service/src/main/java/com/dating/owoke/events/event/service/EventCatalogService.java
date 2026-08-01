package com.dating.owoke.events.event.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.events.event.domain.CatalogEvent;
import com.dating.owoke.events.event.domain.EventStatus;
import com.dating.owoke.events.event.domain.OccurrenceStatus;
import com.dating.owoke.events.event.dto.EventPageResponse;
import com.dating.owoke.events.event.dto.EventResponse;
import com.dating.owoke.events.event.dto.UpdateVenueRequest;
import com.dating.owoke.events.event.exception.EventNotFoundException;
import com.dating.owoke.events.event.mapper.EventMapper;
import com.dating.owoke.events.event.repository.EventRepository;
import com.dating.owoke.events.shared.messaging.event.EventChangedV1;
import com.dating.owoke.events.shared.messaging.service.OutboxService;
import com.dating.owoke.events.sync.dto.ExternalEventData;

import jakarta.persistence.criteria.JoinType;

@Service
public class EventCatalogService {
    private static final String TOPIC = "events.events.v1";
    private final EventRepository repository;
    private final EventMapper mapper;
    private final OutboxService outboxService;
    private final Clock clock;

    public EventCatalogService(EventRepository repository, EventMapper mapper, OutboxService outboxService, Clock clock) {
        this.repository = repository; this.mapper = mapper; this.outboxService = outboxService; this.clock = clock;
    }

    @Transactional
    public UUID upsert(ExternalEventData data) {
        Instant now = clock.instant();
        CatalogEvent event = repository.findBySourceAndExternalId("KUDAGO", data.externalId()).orElse(null);
        EventStatus previous = event == null ? null : event.getStatus();
        if (event == null) {
            event = CatalogEvent.imported(data, now);
        } else {
            event.refresh(data, now);
        }
        repository.save(event);
        publish(eventType(previous, event.getStatus()), event);
        return event.getId();
    }

    @Transactional
    public void finishFullSync(Set<String> seenExternalIds) {
        Instant now = clock.instant();
        for (CatalogEvent event : repository.findAll()) {
            EventStatus before = event.getStatus();
            boolean changed = seenExternalIds.contains(event.getExternalId())
                    ? event.expireOccurrences(now)
                    : event.markMissing(now);
            if (changed) publish(eventType(before, event.getStatus()), event);
        }
    }

    @Transactional(readOnly = true)
    public EventPageResponse publicEvents(
            Instant from, Instant to, String category, Boolean free, int page, int size) {
        Instant effectiveFrom = from == null ? clock.instant() : from;
        int safeSize = Math.min(Math.max(size, 1), 50);
        Specification<CatalogEvent> spec = activeSpecification(effectiveFrom, to, category, free);
        Page<CatalogEvent> result = repository.findAll(spec,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by("updatedAt").descending()));
        return page(result, true);
    }

    @Transactional(readOnly = true)
    public EventResponse publicEvent(UUID id) {
        CatalogEvent event = repository.findDetailedById(id)
                .filter(value -> value.getStatus() == EventStatus.ACTIVE)
                .orElseThrow(() -> new EventNotFoundException(id));
        return mapper.response(event, true, clock.instant());
    }

    @Transactional(readOnly = true)
    public EventPageResponse adminEvents(EventStatus status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Specification<CatalogEvent> spec = status == null ? null
                : (root, query, cb) -> cb.equal(root.get("status"), status);
        Page<CatalogEvent> result = repository.findAll(spec,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by("updatedAt").descending()));
        return page(result, false);
    }

    @Transactional
    public EventResponse updateVenue(UUID id, UpdateVenueRequest request) {
        CatalogEvent event = required(id);
        event.updateVenue(request.venueName(), request.venueAddress(), request.latitude(), request.longitude(), clock.instant());
        publish("EventUpdatedV1", event);
        return mapper.response(event, false, clock.instant());
    }

    @Transactional
    public EventResponse publish(UUID id) {
        CatalogEvent event = required(id); EventStatus before = event.getStatus();
        event.publish(clock.instant()); publish(eventType(before, event.getStatus()), event);
        return mapper.response(event, false, clock.instant());
    }

    @Transactional
    public EventResponse hide(UUID id) {
        CatalogEvent event = required(id); event.hide(clock.instant()); publish("EventHiddenV1", event);
        return mapper.response(event, false, clock.instant());
    }

    @Transactional
    public EventResponse archive(UUID id) {
        CatalogEvent event = required(id); event.archive(clock.instant()); publish("EventArchivedV1", event);
        return mapper.response(event, false, clock.instant());
    }

    private CatalogEvent required(UUID id) {
        return repository.findDetailedById(id).orElseThrow(() -> new EventNotFoundException(id));
    }

    private EventPageResponse page(Page<CatalogEvent> result, boolean publicView) {
        Instant now = clock.instant();
        return new EventPageResponse(result.getContent().stream().map(event -> mapper.response(event, publicView, now)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private Specification<CatalogEvent> activeSpecification(Instant from, Instant to, String category, Boolean free) {
        return (root, query, cb) -> {
            query.distinct(true);
            var occurrence = root.join("occurrences", JoinType.INNER);
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("status"), EventStatus.ACTIVE));
            predicates.add(cb.equal(occurrence.get("status"), OccurrenceStatus.ACTIVE));
            predicates.add(cb.greaterThan(occurrence.get("startsAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(occurrence.get("startsAt"), to));
            if (free != null) predicates.add(cb.equal(root.get("free"), free));
            if (category != null && category.matches("[a-z-]{1,64}")) {
                predicates.add(cb.like(cb.concat(cb.concat(",", root.get("categories")), ","), "%," + category + ",%"));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private String eventType(EventStatus previous, EventStatus current) {
        if (previous == null) return current == EventStatus.ACTIVE ? "EventPublishedV1" : "EventDraftedV1";
        if (previous != EventStatus.ACTIVE && current == EventStatus.ACTIVE) return "EventPublishedV1";
        if (previous != EventStatus.HIDDEN && current == EventStatus.HIDDEN) return "EventHiddenV1";
        if (previous != EventStatus.ARCHIVED && current == EventStatus.ARCHIVED) return "EventArchivedV1";
        return "EventUpdatedV1";
    }

    private void publish(String eventType, CatalogEvent event) {
        var payload = new EventChangedV1(event.getId(), event.getSource(), event.getExternalId(), event.getTitle(),
                event.getDescription(), event.getCategories(), event.getPriceText(), event.isFree(),
                event.getAgeRestriction(), event.getSourcePageUrl(), event.getLocalPlaceId(),
                new EventChangedV1.Venue(event.getVenueName(), event.getVenueAddress(), event.getLatitude(), event.getLongitude()),
                event.getStatus().name(),
                event.getOccurrences().stream().map(item -> new EventChangedV1.Occurrence(item.getId(), item.getStartsAt(),
                        item.getEndsAt(), item.isContinuous(), item.getStatus().name())).toList(),
                event.getImages().stream().map(item -> new EventChangedV1.Image(item.getProviderAssetKey(),
                        item.getRemoteUrl(), item.getThumbnailUrl(), item.getSourceName(), item.getSourceLink())).toList());
        outboxService.enqueue(TOPIC, event.getId(), eventType, payload);
    }
}
