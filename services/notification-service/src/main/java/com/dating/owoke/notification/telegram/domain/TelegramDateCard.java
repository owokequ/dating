package com.dating.owoke.notification.telegram.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "telegram_date_cards")
@IdClass(TelegramDateCardId.class)
public class TelegramDateCard {
    @Id @Column(name = "proposal_id") private UUID proposalId;
    @Id @Column(name = "user_id") private UUID userId;
    @Column(name = "chat_id", nullable = false) private long chatId;
    @Column(name = "message_id", nullable = false) private long messageId;
    @Column(nullable = false, length = 1024) private String caption;
    @Column(name = "action_url", length = 1000) private String actionUrl;
    @Column(name = "media_id") private UUID mediaId;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    protected TelegramDateCard() { }
    public TelegramDateCard(UUID proposalId, UUID userId, long chatId, long messageId, String caption, String actionUrl, UUID mediaId, Instant now) {
        this.proposalId = proposalId; this.userId = userId; this.chatId = chatId; this.messageId = messageId;
        this.caption = caption; this.actionUrl = actionUrl; this.mediaId = mediaId; this.updatedAt = now;
    }
    public void replace(long chatId, long messageId, String caption, String actionUrl, UUID mediaId, Instant now) {
        this.chatId = chatId; this.messageId = messageId; this.caption = caption; this.actionUrl = actionUrl; this.mediaId = mediaId; this.updatedAt = now;
    }
    public UUID getProposalId() { return proposalId; }
    public UUID getUserId() { return userId; }
    public long getChatId() { return chatId; }
    public long getMessageId() { return messageId; }
    public String getCaption() { return caption; }
    public String getActionUrl() { return actionUrl; }
    public UUID getMediaId() { return mediaId; }
}
