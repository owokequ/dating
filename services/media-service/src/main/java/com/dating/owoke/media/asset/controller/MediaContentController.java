package com.dating.owoke.media.asset.controller;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.media.asset.domain.MediaVariantName;
import com.dating.owoke.media.asset.service.MediaContent;
import com.dating.owoke.media.asset.service.MediaContentService;

@RestController
@RequestMapping("/api/v1/media/assets")
public class MediaContentController {

    private final MediaContentService service;

    public MediaContentController(MediaContentService service) {
        this.service = service;
    }

    @GetMapping("/{mediaId}/content")
    ResponseEntity<byte[]> content(
            @PathVariable UUID mediaId,
            @RequestParam(defaultValue = "CARD") MediaVariantName variant) {
        MediaContent content = service.get(mediaId, variant);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header(HttpHeaders.ETAG, '"' + content.sha256() + '"')
                .body(content.content());
    }
}
