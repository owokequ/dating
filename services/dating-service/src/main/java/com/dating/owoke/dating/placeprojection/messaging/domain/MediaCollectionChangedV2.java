package com.dating.owoke.dating.placeprojection.messaging.domain;

import java.util.List;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record MediaCollectionChangedV2(
        String ownerType,
        UUID ownerId,
        UUID coverMediaId,
        List<UUID> orderedMediaIds,
        List<JsonNode> items,
        long collectionVersion) {
}
