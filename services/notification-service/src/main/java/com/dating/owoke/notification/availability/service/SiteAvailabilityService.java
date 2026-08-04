package com.dating.owoke.notification.availability.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.availability.configuration.SiteAvailabilityProperties;
import com.dating.owoke.notification.availability.domain.MonitorKind;
import com.dating.owoke.notification.availability.domain.MonitorStatus;
import com.dating.owoke.notification.availability.dto.SiteAvailabilityWebhookRequest;
import com.dating.owoke.notification.availability.exception.InvalidSiteAvailabilityWebhookException;
import com.dating.owoke.notification.availability.repository.SiteAvailabilityStateRepository;
import com.dating.owoke.notification.contact.repository.ContactProjectionRepository;
import com.dating.owoke.notification.notification.service.NotificationService;
import com.dating.owoke.notification.shared.configuration.NotificationProperties;

@Service
public class SiteAvailabilityService {

    private static final String NOTIFICATION_TYPE = "SITE_AVAILABLE";
    private static final String TITLE = "For my L снова доступен ✨";
    private static final String BODY = "Сайт снова доступен. Можно продолжить выбирать места и планировать свидания.";

    private final SiteAvailabilityProperties properties;
    private final SiteAvailabilityStateRepository stateRepository;
    private final ContactProjectionRepository contactRepository;
    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;

    public SiteAvailabilityService(
            SiteAvailabilityProperties properties,
            SiteAvailabilityStateRepository stateRepository,
            ContactProjectionRepository contactRepository,
            NotificationService notificationService,
            NotificationProperties notificationProperties) {
        this.properties = properties;
        this.stateRepository = stateRepository;
        this.contactRepository = contactRepository;
        this.notificationService = notificationService;
        this.notificationProperties = notificationProperties;
    }

    @Transactional
    public void accept(String suppliedSecret, SiteAvailabilityWebhookRequest request) {
        requireAuthorized(suppliedSecret);
        MonitorKind monitor = resolveMonitor(request.monitorId());
        boolean recovered = stateRepository.lockSingleton()
                .orElseThrow(() -> new IllegalStateException("Site availability state is missing"))
                .apply(monitor, MonitorStatus.fromExternal(request.status()), Instant.ofEpochSecond(request.occurredAt()));
        if (!recovered) {
            return;
        }

        UUID recoveryEventId = UUID.randomUUID();
        contactRepository.findAll().forEach(contact -> notificationService.createTelegramOnly(
                recoveryEventId,
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

    private MonitorKind resolveMonitor(String monitorId) {
        if (properties.frontendMonitorId().equals(monitorId)) {
            return MonitorKind.FRONTEND;
        }
        if (properties.apiMonitorId().equals(monitorId)) {
            return MonitorKind.API;
        }
        throw new InvalidSiteAvailabilityWebhookException();
    }
}
