package com.dating.owoke.notification.media.service;

public record MediaBinary(byte[] content, String contentType, String contentHash) {
}
