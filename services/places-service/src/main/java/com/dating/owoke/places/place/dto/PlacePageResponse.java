package com.dating.owoke.places.place.dto;

import java.util.List;

public record PlacePageResponse(
        List<PlaceResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
