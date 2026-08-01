package com.dating.owoke.media.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dating.owoke.media.asset.repository.MediaAssetRepository;
import com.dating.owoke.media.collection.domain.MediaOwnerType;
import com.dating.owoke.media.collection.domain.PlaceProjection;
import com.dating.owoke.media.collection.domain.PlaceProjectionStatus;
import com.dating.owoke.media.collection.repository.MediaCollectionItemRepository;
import com.dating.owoke.media.collection.repository.MediaCollectionRepository;
import com.dating.owoke.media.collection.repository.PlaceProjectionRepository;
import com.dating.owoke.media.shared.exception.ResourceNotFoundException;

class MediaCollectionQueryServiceTest {

    private final MediaCollectionItemRepository itemRepository = mock(MediaCollectionItemRepository.class);
    private final MediaCollectionRepository collectionRepository = mock(MediaCollectionRepository.class);
    private final MediaAssetRepository assetRepository = mock(MediaAssetRepository.class);
    private final PlaceProjectionRepository placeRepository = mock(PlaceProjectionRepository.class);
    private final MediaCollectionQueryService service = new MediaCollectionQueryService(
            itemRepository, collectionRepository, assetRepository, placeRepository);

    @Test
    void publicEndpointHidesDraftCollection() {
        UUID placeId = UUID.randomUUID();
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(
                new PlaceProjection(placeId, PlaceProjectionStatus.DRAFT, Instant.now())));

        assertThatThrownBy(() -> service.getPublic(placeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminEndpointReadsDraftCollection() {
        UUID placeId = UUID.randomUUID();
        when(placeRepository.existsById(placeId)).thenReturn(true);
        when(itemRepository.findActive(MediaOwnerType.PLACE, placeId)).thenReturn(List.of());
        when(assetRepository.findAllById(List.of())).thenReturn(List.of());
        when(collectionRepository.findById(placeId)).thenReturn(Optional.empty());

        var response = service.getAdmin(placeId);

        assertThat(response.placeId()).isEqualTo(placeId);
        assertThat(response.images()).isEmpty();
    }
}
