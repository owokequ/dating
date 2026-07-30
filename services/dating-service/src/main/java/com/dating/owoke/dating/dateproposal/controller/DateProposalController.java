package com.dating.owoke.dating.dateproposal.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.dating.dateproposal.dto.CreateDateProposalRequest;
import com.dating.owoke.dating.dateproposal.dto.DateProposalResponse;
import com.dating.owoke.dating.dateproposal.mapper.DateProposalMapper;
import com.dating.owoke.dating.dateproposal.service.DateProposalService;
import com.dating.owoke.dating.shared.security.AuthenticatedUserIdResolver;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/date-proposals")
public class DateProposalController {

    private final DateProposalService proposalService;
    private final DateProposalMapper mapper;
    private final AuthenticatedUserIdResolver userIdResolver;

    public DateProposalController(
            DateProposalService proposalService,
            DateProposalMapper mapper,
            AuthenticatedUserIdResolver userIdResolver
    ) {
        this.proposalService = proposalService;
        this.mapper = mapper;
        this.userIdResolver = userIdResolver;
    }

    @PostMapping
    public ResponseEntity<DateProposalResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateDateProposalRequest request
    ) {
        DateProposalResponse response = mapper.toResponse(proposalService.create(
                userIdResolver.resolve(jwt),
                request.scheduledAt(),
                request.placeId(),
                request.description(),
                idempotencyKey));
        return ResponseEntity.created(URI.create("/api/v1/date-proposals/" + response.id())).body(response);
    }

    @GetMapping
    public List<DateProposalResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return proposalService.list(userIdResolver.resolve(jwt)).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{proposalId}")
    public DateProposalResponse get(@PathVariable UUID proposalId, @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(proposalService.get(proposalId, userIdResolver.resolve(jwt)));
    }

    @PostMapping("/{proposalId}/accept")
    public DateProposalResponse accept(
            @PathVariable UUID proposalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return mapper.toResponse(proposalService.accept(
                proposalId, userIdResolver.resolve(jwt), idempotencyKey));
    }

    @PostMapping("/{proposalId}/decline")
    public DateProposalResponse decline(
            @PathVariable UUID proposalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return mapper.toResponse(proposalService.decline(
                proposalId, userIdResolver.resolve(jwt), idempotencyKey));
    }

    @PostMapping("/{proposalId}/cancel")
    public DateProposalResponse cancel(
            @PathVariable UUID proposalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return mapper.toResponse(proposalService.cancel(
                proposalId, userIdResolver.resolve(jwt), idempotencyKey));
    }
}
