package com.dating.owoke.notification.preference.controller;
import java.util.UUID; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*;
import com.dating.owoke.notification.preference.dto.*; import com.dating.owoke.notification.preference.service.NotificationPreferenceService;
@RestController @RequestMapping("/api/v1/notification-preferences") public class NotificationPreferenceController {
 private final NotificationPreferenceService service; public NotificationPreferenceController(NotificationPreferenceService service) { this.service = service; }
 @GetMapping public NotificationPreferenceResponse get(@AuthenticationPrincipal Jwt jwt) { return service.get(UUID.fromString(jwt.getSubject())); }
 @PatchMapping public NotificationPreferenceResponse update(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateNotificationPreferenceRequest request) { return service.update(UUID.fromString(jwt.getSubject()), request); }
}
