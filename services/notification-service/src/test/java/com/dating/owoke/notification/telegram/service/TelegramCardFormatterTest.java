package com.dating.owoke.notification.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramCardFormatterTest {
    private final TelegramCardFormatter formatter = new TelegramCardFormatter();

    @Test
    void formatsRomanticCardAndEscapesUserText() {
        String caption = formatter.format(
                "Новое предложение 💌",
                "📅 Дата: 03.08.2026\n⏰ Время: 19:00\n📍 Кафе, Казань\n\n💭 Столик <у окна> & цветы");

        assertThat(caption)
                .contains("<b>Новое предложение 💌</b>", "📅 Дата: 03.08.2026", "⏰ Время: 19:00", "📍 Кафе, Казань")
                .contains("💭 Столик &lt;у окна&gt; &amp; цветы")
                .contains("С любовью, For my L ✨")
                .hasSizeLessThanOrEqualTo(1024);
    }

    @Test
    void keepsClosingHtmlWhenLongDescriptionIsTruncated() {
        String caption = formatter.format("Свидание", "Дата — Место\n" + "&".repeat(2000));

        assertThat(caption).hasSizeLessThanOrEqualTo(1024).endsWith("</i>");
    }
}
