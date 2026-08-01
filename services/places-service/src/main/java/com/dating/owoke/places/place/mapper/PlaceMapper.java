package com.dating.owoke.places.place.mapper;

import org.springframework.stereotype.Component;

import com.dating.owoke.places.place.domain.Place;
import com.dating.owoke.places.place.dto.PlaceResponse;
import com.dating.owoke.places.media.service.PlaceMediaView;

@Component
public class PlaceMapper {

    public PlaceResponse toResponse(Place place) {
        return toResponse(place, PlaceMediaView.empty());
    }

    public PlaceResponse toResponse(Place place, PlaceMediaView media) {
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
                place.getSourcePageUrl(),
                place.getSource() == com.dating.owoke.places.place.domain.PlaceSource.KUDAGO ? "KudaGo" : null,
                place.getProviderDescription(),
                place.isDescriptionOverridden(),
                place.getStatus().name(),
                media.coverMediaId(),
                media.images(),
                place.getCreatedAt(),
                place.getUpdatedAt(),
                place.getVersion());
    }
}
