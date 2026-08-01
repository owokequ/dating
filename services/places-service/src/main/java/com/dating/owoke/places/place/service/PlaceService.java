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
import com.dating.owoke.places.media.service.PlaceMediaQueryService;
import com.dating.owoke.places.shared.messaging.event.ExternalImageV2;
import com.dating.owoke.places.shared.messaging.event.PlaceChangedV2;
import com.dating.owoke.places.shared.messaging.service.OutboxService;

@Service
public class PlaceService {

    private static final String PLACES_EVENTS_TOPIC = "places.events.v1";

    private final PlaceRepository repository;
    private final OutboxService outboxService;
    private final PlaceMediaQueryService mediaQueryService;
    private final Clock clock;

    public PlaceService(
            PlaceRepository repository,
            OutboxService outboxService,
            PlaceMediaQueryService mediaQueryService,
            Clock clock) {
        this.repository = repository;
        this.outboxService = outboxService;
        this.mediaQueryService = mediaQueryService;
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
        if (place.getSource().isExternal()
                && previousStatus != PlaceStatus.ACTIVE
                && request.status() == PlaceStatus.ACTIVE
                && !mediaQueryService.hasCover(placeId)) {
            throw new IllegalArgumentException("External place requires a cover image before publication");
        }
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
    public UpsertResult upsertExternal(ExternalPlaceData data) {
        if (!data.source().isExternal()) {
            throw new IllegalArgumentException("External upsert requires an external source");
        }
        Place existing = repository.findBySourceAndExternalId(data.source(), data.externalId()).orElse(null);
        if (existing == null) {
            boolean duplicate = repository.findFirstByNormalizedNameAndNormalizedAddress(
                            Place.normalize(data.name()), Place.normalize(data.address()))
                    .isPresent();
            if (duplicate) {
                return UpsertResult.DUPLICATE;
            }
            Place place = repository.save(Place.external(
                    data.source(),
                    data.externalId(),
                    data.name(),
                    data.providerDescription(),
                    data.category(),
                    data.address(),
                    data.latitude(),
                    data.longitude(),
                    data.sourcePageUrl(),
                    data.imagesFingerprint(),
                    clock.instant()));
            publish("PlaceDraftedV2", place, externalImages(data));
            return UpsertResult.CREATED;
        }

        PlaceStatus previousStatus = existing.getStatus();
        boolean changed = existing.refreshExternal(
                data.name(),
                data.providerDescription(),
                data.category(),
                data.address(),
                data.latitude(),
                data.longitude(),
                data.sourcePageUrl(),
                data.imagesFingerprint(),
                clock.instant());
        if (!changed) {
            return UpsertResult.UNCHANGED;
        }
        publishIfRequired(eventType(previousStatus, existing.getStatus()), existing, externalImages(data));
        return changed ? UpsertResult.UPDATED : UpsertResult.UNCHANGED;
    }

    @Transactional
    public UpsertResult upsertTwoGis(ExternalPlaceData data) {
        if (data.source() != PlaceSource.TWO_GIS) {
            throw new IllegalArgumentException("2GIS upsert requires TWO_GIS source");
        }
        return upsertExternal(data);
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
        publish(eventType, place, mediaQueryService.findExternalImages(place.getId()));
    }

    private void publish(String eventType, Place place, java.util.List<ExternalImageV2> images) {
        outboxService.enqueue(
                PLACES_EVENTS_TOPIC,
                place.getId(),
                eventType,
                2,
                new PlaceChangedV2(
                        place.getId(),
                        place.getCityCode(),
                        place.getName(),
                        place.getAddress(),
                        place.getCategory(),
                        place.getLatitude(),
                        place.getLongitude(),
                        place.getPriceLevel(),
                        place.getStatus().name(),
                        place.getSource().name(),
                        place.getExternalId(),
                        place.getSourcePageUrl(),
                        images));
    }

    private void publishIfRequired(String eventType, Place place) {
        if (eventType != null) {
            publish(eventType, place);
        }
    }

    private void publishIfRequired(String eventType, Place place, java.util.List<ExternalImageV2> images) {
        if (eventType != null) {
            publish(eventType, place, images);
        }
    }

    private String eventType(PlaceStatus previous, PlaceStatus current) {
        if (current == PlaceStatus.DRAFT) {
            return "PlaceDraftedV2";
        }
        if (previous != PlaceStatus.ACTIVE && current == PlaceStatus.ACTIVE) {
            return "PlacePublishedV2";
        }
        if (previous != PlaceStatus.ARCHIVED && current == PlaceStatus.ARCHIVED) {
            return "PlaceArchivedV2";
        }
        if (current == PlaceStatus.ARCHIVED) {
            return null;
        }
        return "PlaceUpdatedV2";
    }

    private java.util.List<ExternalImageV2> externalImages(ExternalPlaceData data) {
        return data.images().stream()
                .limit(5)
                .map(image -> new ExternalImageV2(
                        image.providerAssetKey(), image.remoteUrl(), image.sourceName(), image.sourceLink()))
                .toList();
    }
}
