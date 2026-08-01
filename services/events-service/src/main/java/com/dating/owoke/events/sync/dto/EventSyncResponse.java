package com.dating.owoke.events.sync.dto;

import java.util.List;

public record EventSyncResponse(
        int pages,
        int received,
        int upserted,
        int skipped,
        boolean complete,
        List<EventSyncError> errors) {
}
