package com.dating.owoke.notification.telegram.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dating.owoke.notification.telegram.domain.ReminderCallback;
import com.dating.owoke.notification.telegram.domain.TelegramDateCard;
import com.dating.owoke.notification.telegram.dto.TelegramInlineButton;
import com.dating.owoke.notification.telegram.repository.TelegramDateCardRepository;

@Service
public class TelegramDateCardService {
    private static final String SIGNATURE = "\n\n<i>С любовью, For my L ✨</i>";
    private final TelegramDateCardRepository repository;
    private final TelegramBotClient botClient;
    private final TelegramMediaService mediaService;
    private final Clock clock;

    public TelegramDateCardService(
            TelegramDateCardRepository repository,
            TelegramBotClient botClient,
            TelegramMediaService mediaService,
            Clock clock) {
        this.repository = repository; this.botClient = botClient; this.mediaService = mediaService; this.clock = clock;
    }

    @Transactional
    public void rememberSent(UUID proposalId, UUID userId, long chatId, long messageId, String caption, String actionUrl, UUID mediaId) {
        TelegramDateCard card = repository.findByProposalIdAndUserId(proposalId, userId)
                .orElseGet(() -> new TelegramDateCard(proposalId, userId, chatId, messageId, caption, actionUrl, mediaId, clock.instant()));
        card.replace(chatId, messageId, caption, actionUrl, mediaId, clock.instant());
        repository.save(card);
    }

    @Transactional
    public Optional<String> updateExisting(UUID proposalId, UUID userId, String caption, String actionUrl, List<TelegramInlineButton> buttons) {
        return repository.findByProposalIdAndUserId(proposalId, userId).map(card -> {
            return editOrReplace(card, caption, actionUrl, buttons);
        });
    }

    @Transactional
    public void transitionDecision(UUID proposalId, UUID userId, String title, boolean accepted) {
        repository.findByProposalIdAndUserId(proposalId, userId).ifPresent(card -> {
            String caption = replaceTitle(card.getCaption(), title);
            if (accepted) caption = withReminder(caption, "выберите время");
            editOrReplace(card, caption, card.getActionUrl(), accepted ? reminderButtons(proposalId) : List.of());
        });
    }

    @Transactional
    public void showReminderOptions(UUID proposalId, UUID userId, boolean custom) {
        repository.findByProposalIdAndUserId(proposalId, userId).ifPresent(card -> {
            List<TelegramInlineButton> buttons = custom ? customButtons(proposalId) : reminderButtons(proposalId);
            editOrReplace(card, card.getCaption(), card.getActionUrl(), buttons);
        });
    }

    @Transactional
    public void setReminder(UUID proposalId, UUID userId, String value, boolean enabled) {
        repository.findByProposalIdAndUserId(proposalId, userId).ifPresent(card -> {
            String caption = withReminder(card.getCaption(), enabled ? value : "отключено");
            List<TelegramInlineButton> buttons = enabled ? List.of(
                    new TelegramInlineButton("Изменить", new ReminderCallback("edit", proposalId).encode()),
                    new TelegramInlineButton("Отключить", new ReminderCallback("off", proposalId).encode())) : reminderButtons(proposalId);
            editOrReplace(card, caption, card.getActionUrl(), buttons);
        });
    }

    public static List<TelegramInlineButton> reminderButtons(UUID proposalId) {
        return List.of(new TelegramInlineButton("За 3 часа", new ReminderCallback("3h", proposalId).encode()),
                new TelegramInlineButton("За 1 час", new ReminderCallback("1h", proposalId).encode()),
                new TelegramInlineButton("За 30 минут", new ReminderCallback("30m", proposalId).encode()),
                new TelegramInlineButton("Своё время", new ReminderCallback("pick", proposalId).encode()));
    }
    public static List<TelegramInlineButton> customButtons(UUID proposalId) {
        return List.of(new TelegramInlineButton("15 минут", new ReminderCallback("15m", proposalId).encode()),
                new TelegramInlineButton("45 минут", new ReminderCallback("45m", proposalId).encode()),
                new TelegramInlineButton("90 минут", new ReminderCallback("90m", proposalId).encode()),
                new TelegramInlineButton("Ввести минуты", new ReminderCallback("text", proposalId).encode()));
    }

    private String editOrReplace(
            TelegramDateCard card,
            String caption,
            String actionUrl,
            List<TelegramInlineButton> buttons) {
        try {
            botClient.editPhotoCaption(card.getChatId(), card.getMessageId(), caption, actionUrl, buttons);
            card.replace(card.getChatId(), card.getMessageId(), caption, actionUrl, card.getMediaId(), clock.instant());
            return Long.toString(card.getMessageId());
        } catch (RuntimeException ignored) {
            TelegramMediaService.PreparedTelegramPhoto prepared = card.getMediaId() == null
                    ? mediaService.preparePlaceholder() : mediaService.prepare(card.getMediaId());
            TelegramPhotoResult replacement = botClient.sendPhoto(
                    card.getChatId(), caption, actionUrl, buttons, prepared.photo());
            mediaService.remember(prepared, replacement);
            card.replace(card.getChatId(), Long.parseLong(replacement.messageId()), caption,
                    actionUrl, card.getMediaId(), clock.instant());
            return replacement.messageId();
        }
    }
    private static String replaceTitle(String caption, String title) {
        int end = caption.indexOf("</b>");
        return end < 0 ? "<b>" + title + "</b>\n\n" + caption : "<b>" + title + caption.substring(end);
    }
    private static String withReminder(String caption, String value) {
        String without = caption.replaceAll("(?s)\\n\\n⏰ Напоминание:.*?(?=\\n\\n<i>|$)", "");
        int signature = without.lastIndexOf("\n\n<i>");
        String main = signature < 0 ? without : without.substring(0, signature);
        String suffix = signature < 0 ? SIGNATURE : without.substring(signature);
        return main + "\n\n⏰ Напоминание: " + value + suffix;
    }
}
