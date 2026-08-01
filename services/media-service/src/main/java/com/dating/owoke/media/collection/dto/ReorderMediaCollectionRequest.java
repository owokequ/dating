package com.dating.owoke.media.collection.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReorderMediaCollectionRequest(
        @NotNull UUID coverMediaId,
        @NotEmpty @Size(max = 5) List<@NotNull UUID> orderedMediaIds) {
}
