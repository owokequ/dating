package com.dating.owoke.places.media.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.places.media.domain.PlaceMediaProjectionItem;
import com.dating.owoke.places.media.repository.PlaceMediaProjectionItemRepository;
import com.dating.owoke.places.place.dto.PlaceImageResponse;

@Service
public class PlaceMediaQueryService {

    private final PlaceMediaProjectionItemRepository repository;

    public PlaceMediaQueryService(PlaceMediaProjectionItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, PlaceMediaView> findByPlaceIds(Collection<UUID> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<PlaceMediaProjectionItem>> grouped = repository
                .findByPlaceIdInOrderByPlaceIdAscPositionAsc(placeIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        PlaceMediaProjectionItem::getPlaceId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
        Map<UUID, PlaceMediaView> result = new LinkedHashMap<>();
        grouped.forEach((placeId, items) -> result.put(placeId, view(items)));
        return result;
    }

    private PlaceMediaView view(List<PlaceMediaProjectionItem> items) {
        UUID cover = items.stream().filter(PlaceMediaProjectionItem::isCover)
                .map(PlaceMediaProjectionItem::getMediaId).findFirst().orElse(null);
        List<PlaceImageResponse> images = items.stream().map(item -> {
            String base = "/api/v1/media/assets/" + item.getMediaId() + "/content?variant=";
            return new PlaceImageResponse(
                    item.getMediaId(),
                    item.getPosition(),
                    item.isCover(),
                    base + "THUMBNAIL",
                    base + "CARD",
                    base + "DETAIL");
        }).toList();
        return new PlaceMediaView(cover, images);
    }
}
