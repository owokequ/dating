package com.dating.owoke.identity.shared.messaging.service;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.identity.shared.messaging.domain.OutboxEvent;
import com.dating.owoke.identity.shared.messaging.repository.OutboxEventRepository;

@Component
@ConditionalOnProperty(prefix = "owoke.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    public OutboxDispatcher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${owoke.outbox.fixed-delay:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = repository.lockPending(clock.instant(), PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                event.markPublished(clock.instant());
            } catch (Exception exception) {
                event.markFailed(exception, clock.instant());
            }
        }
    }
}
