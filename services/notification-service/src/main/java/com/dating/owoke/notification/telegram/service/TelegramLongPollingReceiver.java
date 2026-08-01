package com.dating.owoke.notification.telegram.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
@ConditionalOnProperty(prefix = "owoke.telegram", name = "mode", havingValue = "polling")
public class TelegramLongPollingReceiver {

    private static final String OFFSET_KEY = "owoke:notification:telegram:update-offset";

    private final TelegramBotClient botClient;
    private final TelegramUpdateService updateService;
    private final StringRedisTemplate redisTemplate;

    public TelegramLongPollingReceiver(
            TelegramBotClient botClient,
            TelegramUpdateService updateService,
            StringRedisTemplate redisTemplate) {
        this.botClient = botClient;
        this.updateService = updateService;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(
            fixedDelayString = "${owoke.telegram.polling-fixed-delay:1000}",
            scheduler = "telegramPollingTaskScheduler")
    public void poll() {
        long offset = currentOffset();
        for (JsonNode update : botClient.getUpdates(offset)) {
            updateService.process(update);
            offset = Math.max(offset, update.path("update_id").asLong() + 1);
            redisTemplate.opsForValue().set(OFFSET_KEY, Long.toString(offset));
        }
    }

    private long currentOffset() {
        String value = redisTemplate.opsForValue().get(OFFSET_KEY);
        return value == null ? 0 : Long.parseLong(value);
    }
}
