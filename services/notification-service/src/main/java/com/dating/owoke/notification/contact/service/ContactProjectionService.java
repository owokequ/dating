package com.dating.owoke.notification.contact.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dating.owoke.notification.contact.domain.ContactProjection;
import com.dating.owoke.notification.contact.repository.ContactProjectionRepository;
import com.dating.owoke.notification.preference.domain.NotificationPreference;
import com.dating.owoke.notification.preference.repository.NotificationPreferenceRepository;

@Service
public class ContactProjectionService {

    private final ContactProjectionRepository contactRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final Clock clock;

    public ContactProjectionService(
            ContactProjectionRepository contactRepository,
            NotificationPreferenceRepository preferenceRepository,
            Clock clock) {
        this.contactRepository = contactRepository;
        this.preferenceRepository = preferenceRepository;
        this.clock = clock;
    }

    public void register(UUID userId, String displayName, String email) {
        ContactProjection contact = contactRepository.findById(userId).orElse(null);
        if (contact == null) {
            contactRepository.save(new ContactProjection(userId, displayName, email, clock.instant()));
            preferenceRepository.save(new NotificationPreference(userId, clock.instant()));
        } else {
            contact.updateProfile(displayName, email, clock.instant());
        }
    }

    public void updateProfile(UUID userId, String displayName) {
        ContactProjection contact = required(userId);
        contact.updateProfile(displayName, null, clock.instant());
    }

    public void linkTelegram(UUID userId, long telegramUserId, Long chatId, String username, boolean botAccess) {
        required(userId).linkTelegram(telegramUserId, chatId, username, botAccess, clock.instant());
    }

    public ContactProjection required(UUID userId) {
        return contactRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Contact projection is missing for user " + userId));
    }
}
