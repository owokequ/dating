package com.dating.owoke.events.sync.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KudaGoProperties.class)
public class KudaGoConfiguration {
}
