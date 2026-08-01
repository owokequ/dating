package com.dating.owoke.events.sync.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.dating.owoke.events.event.service.EventCatalogService;
import com.dating.owoke.events.sync.configuration.KudaGoProperties;
import com.dating.owoke.events.sync.dto.EventSyncError;
import com.dating.owoke.events.sync.dto.EventSyncResponse;
import com.dating.owoke.events.sync.exception.SyncUnavailableException;

@Service
public class EventSyncService {
    private final KudaGoEventClient client;
    private final EventSyncLock lock;
    private final EventCatalogService catalogService;
    private final KudaGoProperties properties;
    private final Clock clock;

    public EventSyncService(KudaGoEventClient client, EventSyncLock lock, EventCatalogService catalogService,
            KudaGoProperties properties, Clock clock) {
        this.client = client; this.lock = lock; this.catalogService = catalogService;
        this.properties = properties; this.clock = clock;
    }

    public EventSyncResponse synchronize() {
        String token = lock.acquire();
        try { return doSynchronize(); } finally { lock.release(token); }
    }

    private EventSyncResponse doSynchronize() {
        Instant from = clock.instant(); Instant until = from.plus(properties.horizon());
        int pages = 0, received = 0, upserted = 0, skipped = 0;
        boolean complete = true; boolean next = true;
        Set<String> seen = new HashSet<>(); List<EventSyncError> errors = new ArrayList<>();
        for (int page = 1; page <= 20 && next; page++) {
            try {
                var result = client.page(page, from, until);
                pages++; received += result.received(); skipped += result.skipped(); next = result.hasNext();
                for (var event : result.events()) {
                    try {
                        catalogService.upsert(event); seen.add(event.externalId()); upserted++;
                    } catch (RuntimeException exception) {
                        complete = false; skipped++;
                        errors.add(new EventSyncError(page, "Event " + event.externalId() + " was rejected"));
                    }
                }
            } catch (SyncUnavailableException exception) {
                complete = false; errors.add(new EventSyncError(page, exception.getMessage())); break;
            }
        }
        if (next) complete = false;
        if (pages == 0) throw new SyncUnavailableException("KudaGo synchronization did not complete any request");
        if (complete) catalogService.finishFullSync(seen);
        return new EventSyncResponse(pages, received, upserted, skipped, complete, List.copyOf(errors));
    }
}
