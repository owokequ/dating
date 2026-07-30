package com.dating.owoke.dating.couple.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dating.owoke.dating.couple.dto.CoupleResponse;
import com.dating.owoke.dating.couple.mapper.CoupleMapper;
import com.dating.owoke.dating.couple.service.CoupleService;
import com.dating.owoke.dating.shared.security.AuthenticatedUserIdResolver;

@RestController
@RequestMapping("/api/v1/couples/current")
public class CoupleController {

    private final CoupleService coupleService;
    private final CoupleMapper mapper;
    private final AuthenticatedUserIdResolver userIdResolver;

    public CoupleController(
            CoupleService coupleService,
            CoupleMapper mapper,
            AuthenticatedUserIdResolver userIdResolver
    ) {
        this.coupleService = coupleService;
        this.mapper = mapper;
        this.userIdResolver = userIdResolver;
    }

    @GetMapping
    public CoupleResponse current(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(coupleService.getCurrent(userIdResolver.resolve(jwt)));
    }

    @PostMapping("/close")
    public ResponseEntity<Void> close(@AuthenticationPrincipal Jwt jwt) {
        coupleService.closeCurrent(userIdResolver.resolve(jwt));
        return ResponseEntity.noContent().build();
    }
}
