package com.dating.owoke.media.collection.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dating.owoke.media.asset.dto.MediaUploadResponse;
import com.dating.owoke.media.asset.service.MediaUploadService;

@RestController
@RequestMapping("/api/v1/media/date-proposals")
public class PrivateDateProposalMediaController {

    private final MediaUploadService uploadService;

    public PrivateDateProposalMediaController(MediaUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(path = "/{proposalId}/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    MediaUploadResponse upload(
            @PathVariable UUID proposalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) {
        return uploadService.uploadPrivateDateProposal(proposalId, UUID.fromString(jwt.getSubject()), file);
    }
}
