package com.dating.owoke.notification.telegram.service;

public record TelegramPhoto(byte[] content, String contentType, String fileName, String cachedFileId) {
    public static TelegramPhoto upload(byte[] content, String contentType, String fileName) {
        return new TelegramPhoto(content, contentType, fileName, null);
    }

    public static TelegramPhoto cached(String fileId) {
        return new TelegramPhoto(null, null, null, fileId);
    }

    public boolean cached() {
        return cachedFileId != null;
    }
}
