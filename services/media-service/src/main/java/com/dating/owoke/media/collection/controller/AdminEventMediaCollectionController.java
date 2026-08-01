package com.dating.owoke.media.collection.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dating.owoke.media.asset.dto.MediaUploadResponse;
import com.dating.owoke.media.asset.service.MediaUploadService;
import com.dating.owoke.media.collection.dto.MediaCollectionResponse;
import com.dating.owoke.media.collection.dto.ReorderMediaCollectionRequest;
import com.dating.owoke.media.collection.service.MediaCollectionCommandService;
import com.dating.owoke.media.collection.service.MediaCollectionQueryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/media/event-collections")
public class AdminEventMediaCollectionController {

    private final MediaUploadService uploadService;
    private final MediaCollectionCommandService commandService;
    private final MediaCollectionQueryService queryService;

    public AdminEventMediaCollectionController(
            MediaUploadService uploadService,
            MediaCollectionCommandService commandService,
            MediaCollectionQueryService queryService) {
        this.uploadService = uploadService;
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping("/{eventId}")
    MediaCollectionResponse get(@PathVariable UUID eventId) {
        return queryService.getAdminEvent(eventId);
    }

    @PostMapping(path = "/{eventId}/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    MediaUploadResponse upload(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) {
        return uploadService.uploadEvent(eventId, UUID.fromString(jwt.getSubject()), file);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reorder(@PathVariable UUID eventId, @Valid @RequestBody ReorderMediaCollectionRequest request) {
        commandService.reorderEvent(eventId, request);
    }

    @DeleteMapping("/{eventId}/assets/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID eventId, @PathVariable UUID mediaId) {
        commandService.deleteEvent(eventId, mediaId);
    }
}
