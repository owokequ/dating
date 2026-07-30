package com.dating.owoke.notification.shared.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.notification")
public record NotificationProperties(String webAppUrl, String fromEmail) {
}
