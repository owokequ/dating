package com.dating.owoke.media.collection.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.dating.owoke.media.collection.dto.ReorderMediaCollectionRequest;
import com.dating.owoke.media.collection.service.MediaCollectionCommandService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/media/place-collections")
public class AdminMediaCollectionController {

    private final MediaUploadService uploadService;
    private final MediaCollectionCommandService commandService;

    public AdminMediaCollectionController(
            MediaUploadService uploadService,
            MediaCollectionCommandService commandService) {
        this.uploadService = uploadService;
        this.commandService = commandService;
    }

    @PostMapping(path = "/{placeId}/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    MediaUploadResponse upload(
            @PathVariable UUID placeId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) {
        return uploadService.upload(placeId, UUID.fromString(jwt.getSubject()), file);
    }

    @PatchMapping("/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reorder(@PathVariable UUID placeId, @Valid @RequestBody ReorderMediaCollectionRequest request) {
        commandService.reorder(placeId, request);
    }

    @DeleteMapping("/{placeId}/assets/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID placeId, @PathVariable UUID mediaId) {
        commandService.delete(placeId, mediaId);
    }
}
