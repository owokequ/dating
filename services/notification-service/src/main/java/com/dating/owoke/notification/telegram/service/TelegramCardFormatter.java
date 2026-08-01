package com.dating.owoke.notification.telegram.service;

import org.springframework.stereotype.Component;

@Component
public class TelegramCardFormatter {
    private static final int CAPTION_LIMIT = 1024;

    public String format(String title, String body) {
        String escapedTitle = escape(title);
        String[] bodyParts = body.split("\\R", 2);
        String primary = escape(bodyParts[0]);
        int separator = primary.indexOf(" — ");
        String escapedBody;
        if (separator >= 0) {
            escapedBody = "🗓 " + primary.substring(0, separator)
                    + "\n📍 " + primary.substring(separator + 3);
        } else {
            escapedBody = "✨ " + primary;
        }
        if (bodyParts.length > 1 && !bodyParts[1].isBlank()) {
            escapedBody += "\n\n💭 " + escape(bodyParts[1]);
        }
        String prefix = "<b>" + escapedTitle + "</b>\n\n";
        String suffix = "\n\n<i>С любовью, Owoke ✨</i>";
        int available = CAPTION_LIMIT - prefix.length() - suffix.length();
        if (escapedBody.length() > available) {
            escapedBody = safeHtmlCut(escapedBody, available - 1) + "…";
        }
        return prefix + escapedBody + suffix;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String safeHtmlCut(String value, int length) {
        String cut = value.substring(0, Math.max(0, length));
        int lastAmpersand = cut.lastIndexOf('&');
        int lastSemicolon = cut.lastIndexOf(';');
        return lastAmpersand > lastSemicolon ? cut.substring(0, lastAmpersand) : cut;
    }
}
