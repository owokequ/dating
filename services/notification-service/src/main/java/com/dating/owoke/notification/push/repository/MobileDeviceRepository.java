package com.dating.owoke.notification.push.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dating.owoke.notification.push.domain.MobileDevice;

public interface MobileDeviceRepository extends JpaRepository<MobileDevice, UUID> {
    List<MobileDevice> findByUserIdAndActiveTrue(UUID userId);
    Optional<MobileDevice> findByExpoPushToken(String expoPushToken);
}
