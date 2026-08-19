package com.dating.owoke.notification.push.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("owoke.expo-push")
public record ExpoPushProperties(String baseUrl, String accessToken, boolean enabled) { }
