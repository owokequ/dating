package com.dating.owoke.notification.shared.messaging.listener;

import java.time.Clock;
import java.util.UUID;

import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.shared.messaging.domain.EventEnvelope;
import com.dating.owoke.notification.shared.messaging.domain.FailedMessage;
import com.dating.owoke.notification.shared.messaging.repository.FailedMessageRepository;
import com.dating.owoke.notification.shared.messaging.service.IncomingEventProcessor;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class KafkaEventListener {

    private final IncomingEventProcessor processor;
    private final FailedMessageRepository failedMessageRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public KafkaEventListener(
            IncomingEventProcessor processor,
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
    @KafkaListener(topics = {"identity.events.v1", "dating.events.v1", "notification.commands.v1"})
    public void onEvent(String message, @Header("kafka_receivedTopic") String topic) {
        processor.process(topic, message);
    }

    @DltHandler
    @Transactional
    public void onDlt(String message, @Header("kafka_receivedTopic") String topic, Headers headers) {
        EventEnvelope envelope = tryDeserialize(message);
        failedMessageRepository.save(new FailedMessage(
                envelope == null ? null : envelope.eventId(),
                topic,
                envelope == null ? null : envelope.eventType(),
                message,
                "Kafka retries exhausted",
                clock.instant()));
    }

    private EventEnvelope tryDeserialize(String message) {
        try {
            return objectMapper.readValue(message, EventEnvelope.class);
        } catch (JacksonException exception) {
            return null;
        }
    }
}
