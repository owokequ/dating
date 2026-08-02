package com.dating.owoke.media.collection.messaging.listener;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.dating.owoke.media.collection.messaging.service.PrivateDateDraftEventProcessor;

@Component
@ConditionalOnProperty(prefix = "owoke.messaging", name = "consumers-enabled", havingValue = "true", matchIfMissing = true)
public class PrivateDateDraftEventListener {

    private final PrivateDateDraftEventProcessor processor;

    public PrivateDateDraftEventListener(PrivateDateDraftEventProcessor processor) {
        this.processor = processor;
    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 2000),
            retryTopicSuffix = ".retry",
            dltTopicSuffix = ".dlt",
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            kafkaTemplate = "kafkaTemplate")
    @KafkaListener(topics = "dating.events.v1")
    public void onEvent(String message, @Header("kafka_receivedTopic") String topic) {
        processor.process(topic, message);
    }
}
