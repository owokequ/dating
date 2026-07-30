package com.dating.owoke.notification.notification.mapper;

import org.springframework.stereotype.Component;

import com.dating.owoke.notification.notification.domain.Notification;
import com.dating.owoke.notification.notification.dto.NotificationResponse;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getActionUrl(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
