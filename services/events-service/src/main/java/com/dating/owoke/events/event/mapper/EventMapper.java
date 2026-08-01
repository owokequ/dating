package com.dating.owoke.events.event.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.dating.owoke.events.event.domain.CatalogEvent;
import com.dating.owoke.events.event.dto.EventImageResponse;
import com.dating.owoke.events.event.dto.EventOccurrenceResponse;
import com.dating.owoke.events.event.dto.EventResponse;

@Component
public class EventMapper {
    public EventResponse response(CatalogEvent event, boolean publicView, Instant now) {
        var occurrences = event.getOccurrences().stream()
                .filter(item -> !publicView || item.getStatus().name().equals("ACTIVE"))
                .filter(item -> !publicView || (item.getEndsAt() == null
                        ? item.getStartsAt().isAfter(now)
                        : item.getEndsAt().isAfter(now)))
                .map(item -> new EventOccurrenceResponse(item.getId(), item.getStartsAt(), item.getEndsAt(),
                        item.isContinuous(), item.getStatus().name()))
                .toList();
        var images = event.getImages().stream()
                .map(item -> new EventImageResponse(item.getProviderAssetKey(), item.getRemoteUrl(),
                        item.getThumbnailUrl(), item.getSourceName(), item.getSourceLink()))
                .toList();
        return new EventResponse(event.getId(), event.getTitle(), event.getDescription(),
                event.getProviderDescription(), event.isDescriptionOverridden(), event.getCategories(),
                event.getPriceText(), event.isFree(), event.getAgeRestriction(), event.getSourcePageUrl(),
                event.getVenueName(), event.getVenueAddress(), event.getLatitude(), event.getLongitude(),
                event.isVenueOverride(), event.getLocalPlaceId(), event.getStatus().name(), occurrences, images,
                event.getCreatedAt(), event.getUpdatedAt(), event.getVersion());
    }
}
