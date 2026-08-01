package com.dating.owoke.places.place.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.places.place.domain.Place;
import com.dating.owoke.places.place.domain.PlaceSource;
import com.dating.owoke.places.place.domain.PlaceStatus;
import com.dating.owoke.places.place.dto.CreatePlaceRequest;
import com.dating.owoke.places.place.dto.UpdatePlaceRequest;
import com.dating.owoke.places.place.exception.DuplicatePlaceException;
import com.dating.owoke.places.place.exception.PlaceNotFoundException;
import com.dating.owoke.places.place.repository.PlaceRepository;
import com.dating.owoke.places.shared.messaging.event.PlaceChangedV1;
import com.dating.owoke.places.shared.messaging.service.OutboxService;

@Service
public class PlaceService {

    private static final String PLACES_EVENTS_TOPIC = "places.events.v1";

    private final PlaceRepository repository;
    private final OutboxService outboxService;
    private final Clock clock;

    public PlaceService(PlaceRepository repository, OutboxService outboxService, Clock clock) {
        this.repository = repository;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Place> list(String category, String query, int page, int size) {
        Specification<Place> specification = (root, criteria, builder) -> builder.and(
                builder.equal(root.get("cityCode"), Place.CITY_CODE),
                builder.equal(root.get("status"), PlaceStatus.ACTIVE));
        if (category != null && !category.isBlank()) {
            specification = specification.and((root, criteria, builder) ->
                    builder.equal(root.get("category"), category.trim().toUpperCase(java.util.Locale.ROOT)));
        }
        if (query != null && !query.isBlank()) {
            String pattern = "%" + Place.normalize(query) + "%";
            specification = specification.and((root, criteria, builder) -> builder.or(
                    builder.like(root.get("normalizedName"), pattern),
                    builder.like(root.get("normalizedAddress"), pattern)));
        }
        return repository.findAll(
                specification,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public Page<Place> listAdmin(PlaceStatus status, int page, int size) {
        Specification<Place> specification = (root, criteria, builder) ->
                builder.equal(root.get("cityCode"), Place.CITY_CODE);
        if (status != null) {
            specification = specification.and((root, criteria, builder) -> builder.equal(root.get("status"), status));
        }
        return repository.findAll(
                specification,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
                        Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("name"))));
    }

    @Transactional(readOnly = true)
    public Place getActive(UUID placeId) {
        Place place = required(placeId);
        if (place.getStatus() != PlaceStatus.ACTIVE) {
            throw new PlaceNotFoundException();
        }
        return place;
    }

    @Transactional
    public Place createManual(CreatePlaceRequest request) {
        rejectNormalizedDuplicate(request.name(), request.address());
        Place place = repository.save(Place.manual(
                request.name(),
                request.description(),
                request.category(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.priceLevel(),
                clock.instant()));
        publish("PlacePublishedV1", place);
        return place;
    }

    @Transactional
    public Place update(UUID placeId, UpdatePlaceRequest request) {
        Place place = required(placeId);
        PlaceStatus previousStatus = place.getStatus();
        boolean identityChanged = !Place.normalize(place.getName()).equals(Place.normalize(request.name()))
                || !Place.normalize(place.getAddress()).equals(Place.normalize(request.address()));
        if (identityChanged) {
            repository.findFirstByNormalizedNameAndNormalizedAddress(
                            Place.normalize(request.name()), Place.normalize(request.address()))
                    .filter(other -> !other.getId().equals(placeId))
                    .ifPresent(other -> {
                        throw new DuplicatePlaceException();
                    });
        }
        boolean changed = place.update(
                request.name(),
                request.description(),
                request.category(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.priceLevel(),
                request.status(),
                clock.instant());
        if (changed) {
            publishIfRequired(eventType(previousStatus, place.getStatus()), place);
        }
        return place;
    }

    @Transactional
    public UpsertResult upsertTwoGis(ExternalPlaceData data) {
        Place existing = repository.findBySourceAndExternalId(PlaceSource.TWO_GIS, data.externalId()).orElse(null);
        if (existing == null) {
            boolean duplicate = repository.findFirstByNormalizedNameAndNormalizedAddress(
                            Place.normalize(data.name()), Place.normalize(data.address()))
                    .isPresent();
            if (duplicate) {
                return UpsertResult.DUPLICATE;
            }
            Place place = repository.save(Place.twoGis(
                    data.externalId(),
                    data.name(),
                    data.category(),
                    data.address(),
                    data.latitude(),
                    data.longitude(),
                    clock.instant()));
            publish("PlaceDraftedV1", place);
            return UpsertResult.CREATED;
        }

        PlaceStatus previousStatus = existing.getStatus();
        boolean changed = existing.refreshExternal(
                data.name(),
                data.category(),
                data.address(),
                data.latitude(),
                data.longitude(),
                clock.instant());
        if (!changed) {
            return UpsertResult.UNCHANGED;
        }
        publishIfRequired(eventType(previousStatus, existing.getStatus()), existing);
        return UpsertResult.UPDATED;
    }

    private void rejectNormalizedDuplicate(String name, String address) {
        if (repository.findFirstByNormalizedNameAndNormalizedAddress(
                Place.normalize(name), Place.normalize(address)).isPresent()) {
            throw new DuplicatePlaceException();
        }
    }

    private Place required(UUID placeId) {
        return repository.findById(placeId).orElseThrow(PlaceNotFoundException::new);
    }

    private void publish(String eventType, Place place) {
        outboxService.enqueue(
                PLACES_EVENTS_TOPIC,
                place.getId(),
                eventType,
                new PlaceChangedV1(
                        place.getId(),
                        place.getCityCode(),
                        place.getName(),
                        place.getAddress(),
                        place.getCategory(),
                        place.getLatitude(),
                        place.getLongitude(),
                        place.getPriceLevel(),
                        place.getStatus().name()));
    }

    private void publishIfRequired(String eventType, Place place) {
        if (eventType != null) {
            publish(eventType, place);
        }
    }

    private String eventType(PlaceStatus previous, PlaceStatus current) {
        if (current == PlaceStatus.DRAFT) {
            return "PlaceDraftedV1";
        }
        if (previous != PlaceStatus.ACTIVE && current == PlaceStatus.ACTIVE) {
            return "PlacePublishedV1";
        }
        if (previous != PlaceStatus.ARCHIVED && current == PlaceStatus.ARCHIVED) {
            return "PlaceArchivedV1";
        }
        if (current == PlaceStatus.ARCHIVED) {
            return null;
        }
        return "PlaceUpdatedV1";
    }
}
