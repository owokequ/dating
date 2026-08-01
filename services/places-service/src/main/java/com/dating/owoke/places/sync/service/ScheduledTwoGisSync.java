package com.dating.owoke.places.sync.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "owoke.two-gis",
        name = {"enabled", "schedule-enabled"},
        havingValue = "true")
public class ScheduledTwoGisSync {

    private final TwoGisSyncService syncService;

    public ScheduledTwoGisSync(TwoGisSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(
            initialDelayString = "${owoke.two-gis.initial-delay:5m}",
            fixedDelayString = "${owoke.two-gis.sync-fixed-delay:6h}")
    public void synchronize() {
        syncService.synchronize();
    }
}
