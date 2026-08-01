package com.dating.owoke.notification.telegram.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TelegramBotProperties.class)
public class TelegramBotConfiguration {

    @Bean
    RestClient.Builder telegramRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        return scheduler(4, "owoke-scheduling-");
    }

    @Bean
    ThreadPoolTaskScheduler telegramPollingTaskScheduler() {
        return scheduler(1, "telegram-polling-");
    }

    private ThreadPoolTaskScheduler scheduler(int poolSize, String threadNamePrefix) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        return scheduler;
    }
}
