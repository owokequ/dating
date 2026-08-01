package com.dating.owoke.places.place.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.places.place.domain.Place;
import com.dating.owoke.places.place.dto.PlacePageResponse;
import com.dating.owoke.places.place.dto.PlaceResponse;
import com.dating.owoke.places.place.mapper.PlaceMapper;
import com.dating.owoke.places.place.service.PlaceService;
import com.dating.owoke.places.media.service.PlaceMediaQueryService;
import com.dating.owoke.places.media.service.PlaceMediaView;

@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

    private final PlaceService placeService;
    private final PlaceMapper mapper;
    private final PlaceMediaQueryService mediaQueryService;

    public PlaceController(PlaceService placeService, PlaceMapper mapper, PlaceMediaQueryService mediaQueryService) {
        this.placeService = placeService;
        this.mapper = mapper;
        this.mediaQueryService = mediaQueryService;
    }

    @GetMapping
    public PlacePageResponse list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Place> result = placeService.list(category, query, page, size);
        var media = mediaQueryService.findByPlaceIds(
                result.getContent().stream().map(Place::getId).toList());
        return new PlacePageResponse(
                result.getContent().stream()
                        .map(place -> mapper.toResponse(place, media.getOrDefault(place.getId(), PlaceMediaView.empty())))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping("/{placeId}")
    public PlaceResponse get(@PathVariable UUID placeId) {
        Place place = placeService.getActive(placeId);
        PlaceMediaView media = mediaQueryService.findByPlaceIds(java.util.List.of(placeId))
                .getOrDefault(placeId, PlaceMediaView.empty());
        return mapper.toResponse(place, media);
    }
}
