package com.dating.owoke.notification.telegram.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "owoke.delivery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelegramDecisionDispatcher {
    private final TelegramDecisionService decisionService;
    private final TelegramBotClient botClient;

    public TelegramDecisionDispatcher(TelegramDecisionService decisionService, TelegramBotClient botClient) {
        this.decisionService = decisionService;
        this.botClient = botClient;
    }

    @Scheduled(fixedDelayString = "${owoke.notification.delivery-fixed-delay:1000}")
    public void dispatch() {
        for (TelegramDecisionTask task : decisionService.claim()) {
            try {
                botClient.editPhotoCaption(
                        task.chatId(), task.messageId(), task.caption(), task.actionUrl());
                decisionService.complete(task.requestId());
            } catch (Exception exception) {
                decisionService.fail(task.requestId());
            }
        }
    }
}
