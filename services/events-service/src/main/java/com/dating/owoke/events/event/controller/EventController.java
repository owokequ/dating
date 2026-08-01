package com.dating.owoke.events.event.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.events.event.dto.EventPageResponse;
import com.dating.owoke.events.event.dto.EventResponse;
import com.dating.owoke.events.event.service.EventCatalogService;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    private final EventCatalogService service;
    public EventController(EventCatalogService service) { this.service = service; }

    @GetMapping
    EventPageResponse list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean free,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.publicEvents(from, to, category, free, page, size);
    }

    @GetMapping("/{eventId}")
    EventResponse get(@PathVariable UUID eventId) { return service.publicEvent(eventId); }
}
