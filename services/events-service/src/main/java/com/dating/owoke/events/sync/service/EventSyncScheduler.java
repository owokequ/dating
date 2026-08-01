package com.dating.owoke.events.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "owoke.kudago", name = "schedule-enabled", havingValue = "true")
public class EventSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(EventSyncScheduler.class);
    private final EventSyncService service;
    public EventSyncScheduler(EventSyncService service) { this.service = service; }

    @Scheduled(fixedDelayString = "${owoke.kudago.sync-delay:PT6H}")
    public void synchronize() {
        try {
            var response = service.synchronize();
            log.info("KudaGo event sync completed: pages={}, upserted={}, skipped={}, complete={}",
                    response.pages(), response.upserted(), response.skipped(), response.complete());
        } catch (RuntimeException exception) {
            log.warn("KudaGo event sync failed: {}", exception.getMessage());
        }
    }
}
