package com.dating.owoke.places.sync.dto;

import java.util.List;

public record SyncResponse(
        int received,
        int created,
        int updated,
        int unchanged,
        int duplicates,
        List<SyncFailure> failures) {
}
