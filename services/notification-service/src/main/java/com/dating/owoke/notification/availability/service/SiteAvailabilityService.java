package com.dating.owoke.notification.availability.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.availability.configuration.SiteAvailabilityProperties;
import com.dating.owoke.notification.availability.dto.SiteAvailabilityWebhookRequest;
import com.dating.owoke.notification.availability.exception.InvalidSiteAvailabilityWebhookException;
import com.dating.owoke.notification.contact.repository.ContactProjectionRepository;
import com.dating.owoke.notification.notification.service.NotificationService;
import com.dating.owoke.notification.shared.configuration.NotificationProperties;

@Service
public class SiteAvailabilityService {

    private static final String NOTIFICATION_TYPE = "SITE_AVAILABLE";
    private static final String TITLE = "For my L снова доступен ✨";
    private static final String BODY = "Сайт снова доступен. Можно продолжить выбирать места и планировать свидания.";

    private final SiteAvailabilityProperties properties;
    private final ContactProjectionRepository contactRepository;
    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;

    public SiteAvailabilityService(
            SiteAvailabilityProperties properties,
            ContactProjectionRepository contactRepository,
            NotificationService notificationService,
            NotificationProperties notificationProperties) {
        this.properties = properties;
        this.contactRepository = contactRepository;
        this.notificationService = notificationService;
        this.notificationProperties = notificationProperties;
    }

    @Transactional
    public void accept(String suppliedSecret, SiteAvailabilityWebhookRequest request) {
        requireAuthorized(suppliedSecret);
        contactRepository.findAll().forEach(contact -> notificationService.createTelegramOnly(
                request.incidentId(),
                contact.getUserId(),
                NOTIFICATION_TYPE,
                TITLE,
                BODY,
                notificationProperties.webAppUrl()));
    }

    private void requireAuthorized(String suppliedSecret) {
        if (!properties.isConfigured() || suppliedSecret == null || !MessageDigest.isEqual(
                properties.webhookSecret().getBytes(StandardCharsets.UTF_8),
                suppliedSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidSiteAvailabilityWebhookException();
        }
    }

}
