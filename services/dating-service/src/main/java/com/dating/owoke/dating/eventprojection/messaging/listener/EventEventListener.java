package com.dating.owoke.dating.eventprojection.messaging.listener;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.dating.eventprojection.messaging.service.EventEventProcessor;
import com.dating.owoke.dating.shared.messaging.domain.FailedMessage;
import com.dating.owoke.dating.shared.messaging.domain.IncomingEventEnvelope;
import com.dating.owoke.dating.shared.messaging.repository.FailedMessageRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "owoke.messaging", name = "consumers-enabled", havingValue = "true", matchIfMissing = true)
public class EventEventListener {
    private final EventEventProcessor processor; private final FailedMessageRepository failed;
    private final ObjectMapper objectMapper; private final Clock clock;
    public EventEventListener(EventEventProcessor processor, FailedMessageRepository failed,
            ObjectMapper objectMapper, Clock clock) {
        this.processor = processor; this.failed = failed; this.objectMapper = objectMapper; this.clock = clock;
    }
    @RetryableTopic(attempts = "4", backOff = @BackOff(delay = 2000), retryTopicSuffix = ".retry",
            dltTopicSuffix = ".dlt", sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            kafkaTemplate = "kafkaTemplate")
    @KafkaListener(topics = "events.events.v1")
    public void onEvent(String message, @Header("kafka_receivedTopic") String topic) { processor.process(topic, message); }
    @DltHandler @Transactional
    public void onDlt(String message, @Header("kafka_receivedTopic") String topic) {
        IncomingEventEnvelope envelope = null;
        try { envelope = objectMapper.readValue(message, IncomingEventEnvelope.class); } catch (JacksonException ignored) {}
        failed.save(new FailedMessage(envelope == null ? null : envelope.eventId(), topic,
                envelope == null ? null : envelope.eventType(), message, "Kafka retries exhausted", clock.instant()));
    }
}
