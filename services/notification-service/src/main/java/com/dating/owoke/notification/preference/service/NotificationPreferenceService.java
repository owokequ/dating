package com.dating.owoke.notification.preference.service;
import java.time.Clock; import java.util.UUID;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import com.dating.owoke.notification.preference.domain.NotificationPreference;
import com.dating.owoke.notification.preference.dto.NotificationPreferenceResponse;
import com.dating.owoke.notification.preference.dto.UpdateNotificationPreferenceRequest;
import com.dating.owoke.notification.preference.repository.NotificationPreferenceRepository;
@Service public class NotificationPreferenceService {
 private final NotificationPreferenceRepository repository; private final Clock clock;
 public NotificationPreferenceService(NotificationPreferenceRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }
 @Transactional(readOnly = true) public NotificationPreferenceResponse get(UUID userId) { return response(required(userId)); }
 @Transactional public NotificationPreferenceResponse update(UUID userId, UpdateNotificationPreferenceRequest request) { NotificationPreference value = required(userId); value.update(request.inAppEnabled(), request.pushEnabled(), request.telegramEnabled(), request.emailEnabled(), clock.instant()); return response(value); }
 private NotificationPreference required(UUID userId) { return repository.findById(userId).orElseThrow(() -> new IllegalStateException("Notification preferences are missing")); }
 private static NotificationPreferenceResponse response(NotificationPreference value) { return new NotificationPreferenceResponse(value.isInAppEnabled(), value.isPushEnabled(), value.isTelegramEnabled(), value.isEmailEnabled()); }
}
