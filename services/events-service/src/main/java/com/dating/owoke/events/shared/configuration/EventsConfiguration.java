package com.dating.owoke.events.shared.configuration;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class EventsConfiguration {
    @Bean Clock systemClock() { return Clock.systemUTC(); }
    @Bean RestClient.Builder restClientBuilder() { return RestClient.builder(); }
}
