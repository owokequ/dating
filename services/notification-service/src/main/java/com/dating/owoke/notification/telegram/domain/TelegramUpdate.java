package com.dating.owoke.notification.telegram.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "telegram_updates")
public class TelegramUpdate {

    @Id
    @Column(name = "update_id")
    private long updateId;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private long chatId;

    @Column(name = "reply_text", nullable = false, updatable = false, length = 1000)
    private String replyText;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected TelegramUpdate() {
    }

    public TelegramUpdate(long updateId, long chatId, String replyText, Instant processedAt) {
        this.updateId = updateId;
        this.chatId = chatId;
        this.replyText = replyText;
        this.processedAt = processedAt;
    }

    public long getChatId() {
        return chatId;
    }

    public String getReplyText() {
        return replyText;
    }
}
