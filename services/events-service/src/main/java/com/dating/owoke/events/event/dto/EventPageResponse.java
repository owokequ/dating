package com.dating.owoke.events.event.dto;

import java.util.List;

public record EventPageResponse(
        List<EventResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
