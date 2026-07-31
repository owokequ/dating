package com.dating.owoke.places.place.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.places.place.dto.CreatePlaceRequest;
import com.dating.owoke.places.place.dto.PlaceResponse;
import com.dating.owoke.places.place.dto.UpdatePlaceRequest;
import com.dating.owoke.places.place.mapper.PlaceMapper;
import com.dating.owoke.places.place.service.PlaceService;
import com.dating.owoke.places.sync.dto.SyncResponse;
import com.dating.owoke.places.sync.service.TwoGisSyncService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/places")
public class AdminPlaceController {

    private final PlaceService placeService;
    private final PlaceMapper mapper;
    private final TwoGisSyncService syncService;

    public AdminPlaceController(PlaceService placeService, PlaceMapper mapper, TwoGisSyncService syncService) {
        this.placeService = placeService;
        this.mapper = mapper;
        this.syncService = syncService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceResponse create(@Valid @RequestBody CreatePlaceRequest request) {
        return mapper.toResponse(placeService.createManual(request));
    }

    @PutMapping("/{placeId}")
    public PlaceResponse update(@PathVariable UUID placeId, @Valid @RequestBody UpdatePlaceRequest request) {
        return mapper.toResponse(placeService.update(placeId, request));
    }

    @PostMapping("/sync")
    public SyncResponse synchronize() {
        return syncService.synchronize();
    }
}
