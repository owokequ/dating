package com.dating.owoke.identity.shared.messaging.inbox.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.identity.shared.messaging.inbox.domain.InboxEvent;
import com.dating.owoke.identity.shared.messaging.inbox.event.IncomingEventEnvelope;
import com.dating.owoke.identity.shared.messaging.inbox.repository.InboxEventRepository;
import com.dating.owoke.identity.telegram.service.TelegramBotLinkService;
import com.dating.owoke.identity.telegram.service.TelegramLinkRequestedV1;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class IdentityCommandProcessor {

    private final InboxEventRepository inboxRepository;
    private final TelegramBotLinkService telegramLinkService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdentityCommandProcessor(
            InboxEventRepository inboxRepository,
            TelegramBotLinkService telegramLinkService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.inboxRepository = inboxRepository;
        this.telegramLinkService = telegramLinkService;
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
        if (!"TelegramLinkRequestedV1".equals(envelope.eventType())) {
            throw new IllegalArgumentException("Unsupported identity command: " + envelope.eventType());
        }
        telegramLinkService.link(treeToValue(envelope));
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

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid identity command envelope", exception);
        }
    }
}
