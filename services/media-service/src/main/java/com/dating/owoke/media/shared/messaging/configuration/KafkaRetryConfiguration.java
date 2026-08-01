package com.dating.owoke.media.shared.messaging.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;

@Configuration(proxyBeanMethods = false)
@EnableKafkaRetryTopic
public class KafkaRetryConfiguration {
}
