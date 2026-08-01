package com.dating.owoke.notification.telegram.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "telegram_decision_requests")
public class TelegramDecisionRequest {
    @Id
    @Column(name = "request_id")
    private UUID requestId;
    @Column(name = "telegram_update_id", nullable = false, updatable = false, unique = true)
    private long telegramUpdateId;
    @Column(name = "proposal_id", nullable = false, updatable = false)
    private UUID proposalId;
    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;
    @Column(name = "chat_id", nullable = false, updatable = false)
    private long chatId;
    @Column(name = "message_id", nullable = false, updatable = false)
    private long messageId;
    @Column(name = "action_url", updatable = false, length = 1000)
    private String actionUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TelegramDecisionStatus status;
    @Column(name = "result_caption", length = 1024)
    private String resultCaption;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    protected TelegramDecisionRequest() {
    }

    public TelegramDecisionRequest(
            UUID requestId, long telegramUpdateId, UUID proposalId, UUID actorId,
            long chatId, long messageId, String actionUrl, Instant now) {
        this.requestId = requestId;
        this.telegramUpdateId = telegramUpdateId;
        this.proposalId = proposalId;
        this.actorId = actorId;
        this.chatId = chatId;
        this.messageId = messageId;
        this.actionUrl = actionUrl;
        this.status = TelegramDecisionStatus.PENDING_RESULT;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public void result(String caption, Instant now) {
        if (status == TelegramDecisionStatus.COMPLETED) return;
        this.resultCaption = caption;
        this.status = TelegramDecisionStatus.READY;
        this.nextAttemptAt = now;
    }

    public void processing(Instant now) {
        status = TelegramDecisionStatus.PROCESSING;
        nextAttemptAt = now.plus(Duration.ofMinutes(1));
    }

    public void completed(Instant now) {
        status = TelegramDecisionStatus.COMPLETED;
        completedAt = now;
    }

    public void failed(Instant now) {
        attempts++;
        if (attempts >= 5) {
            status = TelegramDecisionStatus.FAILED;
        } else {
            status = TelegramDecisionStatus.READY;
            nextAttemptAt = now.plus(Duration.ofSeconds(1L << attempts));
        }
    }

    public UUID getRequestId() { return requestId; }
    public UUID getProposalId() { return proposalId; }
    public UUID getActorId() { return actorId; }
    public long getChatId() { return chatId; }
    public long getMessageId() { return messageId; }
    public String getActionUrl() { return actionUrl; }
    public String getResultCaption() { return resultCaption; }
}
