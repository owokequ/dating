package com.dating.owoke.dating.couple.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.dating.couple.dto.CoupleResponse;
import com.dating.owoke.dating.couple.dto.InvitationCreationResponse;
import com.dating.owoke.dating.couple.dto.InvitationPreviewResponse;
import com.dating.owoke.dating.couple.mapper.CoupleMapper;
import com.dating.owoke.dating.couple.service.CoupleInvitationService;
import com.dating.owoke.dating.shared.security.AuthenticatedUserIdResolver;

@RestController
@RequestMapping("/api/v1/couple-invitations")
public class CoupleInvitationController {

    private final CoupleInvitationService invitationService;
    private final CoupleMapper mapper;
    private final AuthenticatedUserIdResolver userIdResolver;

    public CoupleInvitationController(
            CoupleInvitationService invitationService,
            CoupleMapper mapper,
            AuthenticatedUserIdResolver userIdResolver
    ) {
        this.invitationService = invitationService;
        this.mapper = mapper;
        this.userIdResolver = userIdResolver;
    }

    @PostMapping
    public ResponseEntity<InvitationCreationResponse> create(@AuthenticationPrincipal Jwt jwt) {
        InvitationCreationResponse response = mapper.toResponse(
                invitationService.create(userIdResolver.resolve(jwt)));
        return ResponseEntity.created(URI.create("/api/v1/couple-invitations/" + response.invitationId()))
                .body(response);
    }

    @GetMapping("/{token}")
    public InvitationPreviewResponse preview(@PathVariable String token) {
        return mapper.toResponse(invitationService.preview(token));
    }

    @PostMapping("/{token}/accept")
    public CoupleResponse accept(@PathVariable String token, @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(invitationService.accept(token, userIdResolver.resolve(jwt)));
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        invitationService.revoke(invitationId, userIdResolver.resolve(jwt));
        return ResponseEntity.noContent().build();
    }
}
