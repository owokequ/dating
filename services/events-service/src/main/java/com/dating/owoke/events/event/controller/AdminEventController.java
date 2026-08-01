package com.dating.owoke.events.event.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.events.event.domain.EventStatus;
import com.dating.owoke.events.event.dto.EventPageResponse;
import com.dating.owoke.events.event.dto.EventResponse;
import com.dating.owoke.events.event.dto.UpdateVenueRequest;
import com.dating.owoke.events.event.service.EventCatalogService;
import com.dating.owoke.events.sync.dto.EventSyncResponse;
import com.dating.owoke.events.sync.service.EventSyncService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/events")
public class AdminEventController {
    private final EventCatalogService service;
    private final EventSyncService syncService;
    public AdminEventController(EventCatalogService service, EventSyncService syncService) {
        this.service = service; this.syncService = syncService;
    }
    @GetMapping
    EventPageResponse list(@RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.adminEvents(status, page, size);
    }
    @PostMapping("/sync") EventSyncResponse sync() { return syncService.synchronize(); }
    @PatchMapping("/{eventId}/venue")
    EventResponse venue(@PathVariable UUID eventId, @Valid @RequestBody UpdateVenueRequest request) {
        return service.updateVenue(eventId, request);
    }
    @PostMapping("/{eventId}/publish") EventResponse publish(@PathVariable UUID eventId) { return service.publish(eventId); }
    @PostMapping("/{eventId}/hide") EventResponse hide(@PathVariable UUID eventId) { return service.hide(eventId); }
    @PostMapping("/{eventId}/archive") EventResponse archive(@PathVariable UUID eventId) { return service.archive(eventId); }
}
