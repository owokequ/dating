package com.dating.owoke.places.place.mapper;

import org.springframework.stereotype.Component;

import com.dating.owoke.places.place.domain.Place;
import com.dating.owoke.places.place.dto.PlaceResponse;

@Component
public class PlaceMapper {

    public PlaceResponse toResponse(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getCityCode(),
                place.getName(),
                place.getDescription(),
                place.getCategory(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPriceLevel(),
                place.getSource().name(),
                place.getExternalId(),
                place.getStatus().name(),
                place.getCreatedAt(),
                place.getUpdatedAt(),
                place.getVersion());
    }
}
