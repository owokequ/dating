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
import com.dating.owoke.places.shared.messaging.event.ExternalImageV2;

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
            return new PlaceImageResponse(
                    item.getMediaId(),
                    item.getPosition(),
                    item.isCover(),
                    item.getThumbnailUrl(),
                    item.getCardUrl(),
                    item.getDetailUrl(),
                    item.getSource(),
                    item.getSourceName(),
                    item.getSourceLink());
        }).toList();
        return new PlaceMediaView(cover, images);
    }

    @Transactional(readOnly = true)
    public boolean hasCover(UUID placeId) {
        return repository.existsByPlaceIdAndCoverTrue(placeId);
    }

    @Transactional(readOnly = true)
    public List<ExternalImageV2> findExternalImages(UUID placeId) {
        return repository.findByPlaceIdInOrderByPlaceIdAscPositionAsc(List.of(placeId)).stream()
                .filter(item -> "REMOTE_URL".equals(item.getSource()))
                .map(item -> new ExternalImageV2(
                        item.getProviderAssetKey(), item.getDetailUrl(), item.getSourceName(), item.getSourceLink()))
                .toList();
    }
}
