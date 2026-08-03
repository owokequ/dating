package com.dating.owoke.identity.shared.messaging.inbox.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.identity.shared.messaging.inbox.domain.InboxEvent;
import com.dating.owoke.identity.shared.messaging.inbox.event.IncomingEventEnvelope;
import com.dating.owoke.identity.shared.messaging.inbox.repository.InboxEventRepository;
import com.dating.owoke.identity.telegram.service.TelegramBotLinkService;
import com.dating.owoke.identity.account.service.AccountService;
import com.dating.owoke.identity.shared.messaging.inbox.event.OnboardingCompletedV1;
import com.dating.owoke.identity.telegram.service.TelegramLinkRequestedV1;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class IdentityCommandProcessor {

    private final InboxEventRepository inboxRepository;
    private final TelegramBotLinkService telegramLinkService;
    private final AccountService accountService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdentityCommandProcessor(
            InboxEventRepository inboxRepository,
            TelegramBotLinkService telegramLinkService, AccountService accountService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.telegramLinkService = telegramLinkService;
        this.accountService = accountService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void process(String topic, String message) {
        IncomingEventEnvelope envelope = read(message, IncomingEventEnvelope.class);
        if (envelope.eventVersion() != 1) {
            throw new IllegalArgumentException("Unsupported command version: " + envelope.eventVersion());
        }
        if (inboxRepository.existsById(envelope.eventId())) {
            return;
        }
        switch (envelope.eventType()) {
            case "TelegramLinkRequestedV1" -> telegramLinkService.link(treeToValue(envelope));
            case "OnboardingCompletedV1" -> accountService.completeOnboarding(
                    treeToValue(envelope, OnboardingCompletedV1.class).userId());
            default -> throw new IllegalArgumentException("Unsupported identity command: " + envelope.eventType());
        }
        inboxRepository.saveAndFlush(new InboxEvent(
                envelope.eventId(), envelope.eventType(), topic, clock.instant()));
    }

    private TelegramLinkRequestedV1 treeToValue(IncomingEventEnvelope envelope) {
        try {
            return objectMapper.treeToValue(envelope.payload(), TelegramLinkRequestedV1.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid Telegram link command", exception);
        }
    }

    private <T> T treeToValue(IncomingEventEnvelope envelope, Class<T> type) {
        try { return objectMapper.treeToValue(envelope.payload(), type); }
        catch (JacksonException exception) { throw new IllegalArgumentException("Invalid identity command", exception); }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid identity command envelope", exception);
        }
    }
}
