package com.dating.owoke.notification.push.service;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dating.owoke.notification.push.domain.MobileDevice;
import com.dating.owoke.notification.push.domain.MobilePlatform;
import com.dating.owoke.notification.push.repository.MobileDeviceRepository;

@Service
public class MobileDeviceService {
    private final MobileDeviceRepository repository; private final Clock clock;
    public MobileDeviceService(MobileDeviceRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }
    @Transactional public void register(UUID userId, String token, MobilePlatform platform) {
        repository.findByExpoPushToken(token).ifPresentOrElse(
                device -> device.refresh(userId, platform, clock.instant()),
                () -> repository.save(new MobileDevice(userId, token, platform, clock.instant())));
    }
    @Transactional public void deactivate(String token) { repository.findByExpoPushToken(token).ifPresent(device -> device.deactivate(clock.instant())); }
}
