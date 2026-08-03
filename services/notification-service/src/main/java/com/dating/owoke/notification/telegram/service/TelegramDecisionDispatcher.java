package com.dating.owoke.notification.telegram.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "owoke.delivery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelegramDecisionDispatcher {
    private final TelegramDecisionService decisionService;
    private final TelegramDateCardService dateCardService;

    public TelegramDecisionDispatcher(TelegramDecisionService decisionService, TelegramDateCardService dateCardService) {
        this.decisionService = decisionService;
        this.dateCardService = dateCardService;
    }

    @Scheduled(fixedDelayString = "${owoke.notification.delivery-fixed-delay:1000}")
    public void dispatch() {
        for (TelegramDecisionTask task : decisionService.claim()) {
            try {
                boolean accepted = task.title().contains("подтверждено");
                dateCardService.transitionDecision(
                        task.proposalId(), task.actorId(), task.title(), accepted);
                decisionService.complete(task.requestId());
            } catch (Exception exception) {
                decisionService.fail(task.requestId());
            }
        }
    }
}
