package com.dating.owoke.notification.push.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.dating.owoke.notification.push.dto.RegisterDeviceRequest;
import com.dating.owoke.notification.push.service.MobileDeviceService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/v1/mobile/devices")
public class MobileDeviceController {
    private final MobileDeviceService service;
    public MobileDeviceController(MobileDeviceService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RegisterDeviceRequest request) {
        service.register(UUID.fromString(jwt.getSubject()), request.expoPushToken(), request.platform());
    }
}
