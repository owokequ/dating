package com.dating.owoke.media.storage.port;

public record StoredObject(byte[] content, String contentType, String etag) {
}
