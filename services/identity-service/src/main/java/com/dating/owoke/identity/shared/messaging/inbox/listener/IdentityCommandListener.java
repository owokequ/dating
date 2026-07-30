package com.dating.owoke.identity.shared.messaging.inbox.listener;

import java.time.Clock;

import org.springframework.kafka.annotation.BackOff;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.identity.shared.messaging.inbox.domain.FailedMessage;
import com.dating.owoke.identity.shared.messaging.inbox.event.IncomingEventEnvelope;
import com.dating.owoke.identity.shared.messaging.inbox.repository.FailedMessageRepository;
import com.dating.owoke.identity.shared.messaging.inbox.service.IdentityCommandProcessor;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "owoke.messaging", name = "consumers-enabled", havingValue = "true", matchIfMissing = true)
public class IdentityCommandListener {

    private final IdentityCommandProcessor processor;
    private final FailedMessageRepository failedMessageRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdentityCommandListener(
            IdentityCommandProcessor processor,
            FailedMessageRepository failedMessageRepository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.processor = processor;
        this.failedMessageRepository = failedMessageRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 2000),
            retryTopicSuffix = ".retry",
            dltTopicSuffix = ".dlt",
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            kafkaTemplate = "kafkaTemplate")
    @KafkaListener(topics = "identity.commands.v1")
    public void onCommand(String message, @Header("kafka_receivedTopic") String topic) {
        processor.process(topic, message);
    }

    @DltHandler
    @Transactional
    public void onDlt(String message, @Header("kafka_receivedTopic") String topic) {
        IncomingEventEnvelope envelope = tryRead(message);
        failedMessageRepository.save(new FailedMessage(
                envelope == null ? null : envelope.eventId(),
                topic,
                envelope == null ? null : envelope.eventType(),
                message,
                "Kafka retries exhausted",
                clock.instant()));
    }

    private IncomingEventEnvelope tryRead(String value) {
        try {
            return objectMapper.readValue(value, IncomingEventEnvelope.class);
        } catch (JacksonException exception) {
            return null;
        }
    }
}
