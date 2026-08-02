package com.dating.owoke.notification.telegram.service;

import org.springframework.stereotype.Component;

@Component
public class TelegramCardFormatter {
    private static final int CAPTION_LIMIT = 1024;

    public String format(String title, String body) {
        String escapedTitle = escape(title);
        String escapedBody = escape(body).strip();
        String prefix = "<b>" + escapedTitle + "</b>\n\n";
        String suffix = "\n\n<i>С любовью, For my L ✨</i>";
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
