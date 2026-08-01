package com.dating.owoke.places.place.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.places.place.dto.CreatePlaceRequest;
import com.dating.owoke.places.place.dto.PlaceResponse;
import com.dating.owoke.places.place.dto.PlacePageResponse;
import com.dating.owoke.places.place.domain.Place;
import com.dating.owoke.places.place.domain.PlaceStatus;
import com.dating.owoke.places.place.dto.UpdatePlaceRequest;
import com.dating.owoke.places.place.mapper.PlaceMapper;
import com.dating.owoke.places.place.service.PlaceService;
import com.dating.owoke.places.sync.dto.SyncResponse;
import com.dating.owoke.places.sync.service.TwoGisSyncService;
import com.dating.owoke.places.sync.service.KudaGoSyncService;
import com.dating.owoke.places.media.service.PlaceMediaQueryService;
import com.dating.owoke.places.media.service.PlaceMediaView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/places")
public class AdminPlaceController {

    private final PlaceService placeService;
    private final PlaceMapper mapper;
    private final TwoGisSyncService twoGisSyncService;
    private final KudaGoSyncService kudaGoSyncService;
    private final PlaceMediaQueryService mediaQueryService;

    public AdminPlaceController(
            PlaceService placeService,
            PlaceMapper mapper,
            TwoGisSyncService twoGisSyncService,
            KudaGoSyncService kudaGoSyncService,
            PlaceMediaQueryService mediaQueryService) {
        this.placeService = placeService;
        this.mapper = mapper;
        this.twoGisSyncService = twoGisSyncService;
        this.kudaGoSyncService = kudaGoSyncService;
        this.mediaQueryService = mediaQueryService;
    }

    @GetMapping
    public PlacePageResponse list(
            @RequestParam(required = false) PlaceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Place> result = placeService.listAdmin(status, page, size);
        var media = mediaQueryService.findByPlaceIds(result.getContent().stream().map(Place::getId).toList());
        return new PlacePageResponse(
                result.getContent().stream()
                        .map(place -> mapper.toResponse(
                                place, media.getOrDefault(place.getId(), PlaceMediaView.empty())))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceResponse create(@Valid @RequestBody CreatePlaceRequest request) {
        return mapper.toResponse(placeService.createManual(request));
    }

    @PutMapping("/{placeId}")
    public PlaceResponse update(@PathVariable UUID placeId, @Valid @RequestBody UpdatePlaceRequest request) {
        Place place = placeService.update(placeId, request);
        PlaceMediaView media = mediaQueryService.findByPlaceIds(java.util.List.of(placeId))
                .getOrDefault(placeId, PlaceMediaView.empty());
        return mapper.toResponse(place, media);
    }

    @PostMapping("/sync/2gis")
    public SyncResponse synchronizeTwoGis() {
        return twoGisSyncService.synchronize();
    }

    @PostMapping("/sync/kudago")
    public SyncResponse synchronizeKudaGo() {
        return kudaGoSyncService.synchronize();
    }
}
