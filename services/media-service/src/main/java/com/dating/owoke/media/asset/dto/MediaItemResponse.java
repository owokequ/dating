package com.dating.owoke.media.asset.dto;

import java.util.UUID;

public record MediaItemResponse(
        UUID mediaId,
        String status,
        int position,
        boolean cover,
        String thumbnailUrl,
        String cardUrl,
        String detailUrl) {
}
