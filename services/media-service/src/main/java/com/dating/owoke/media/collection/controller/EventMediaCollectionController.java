package com.dating.owoke.media.collection.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.media.collection.dto.MediaCollectionResponse;
import com.dating.owoke.media.collection.service.MediaCollectionQueryService;

@RestController
@RequestMapping("/api/v1/media/event-collections")
public class EventMediaCollectionController {

    private final MediaCollectionQueryService service;

    public EventMediaCollectionController(MediaCollectionQueryService service) {
        this.service = service;
    }

    @GetMapping("/{eventId}")
    MediaCollectionResponse get(@PathVariable UUID eventId) {
        return service.getPublicEvent(eventId);
    }
}
