package com.dating.owoke.dating.placeprojection.messaging.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;

@Configuration(proxyBeanMethods = false)
@EnableKafkaRetryTopic
@ConditionalOnProperty(prefix = "owoke.messaging", name = "consumers-enabled", havingValue = "true", matchIfMissing = true)
public class PlaceKafkaConfiguration {
}
