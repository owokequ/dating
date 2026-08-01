package com.dating.owoke.notification.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramCardFormatterTest {
    private final TelegramCardFormatter formatter = new TelegramCardFormatter();

    @Test
    void formatsRomanticCardAndEscapesUserText() {
        String caption = formatter.format(
                "Новое предложение 💌",
                "03.08.2026 19:00 — Кафе, Казань\nСтолик <у окна> & цветы");

        assertThat(caption)
                .contains("<b>Новое предложение 💌</b>", "🗓 03.08.2026 19:00", "📍 Кафе, Казань")
                .contains("💭 Столик &lt;у окна&gt; &amp; цветы")
                .hasSizeLessThanOrEqualTo(1024);
    }

    @Test
    void keepsClosingHtmlWhenLongDescriptionIsTruncated() {
        String caption = formatter.format("Свидание", "Дата — Место\n" + "&".repeat(2000));

        assertThat(caption).hasSizeLessThanOrEqualTo(1024).endsWith("</i>");
    }
}
