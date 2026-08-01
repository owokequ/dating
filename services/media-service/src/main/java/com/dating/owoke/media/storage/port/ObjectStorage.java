package com.dating.owoke.media.storage.port;

public interface ObjectStorage {

    void put(String key, byte[] content, String contentType);

    StoredObject get(String key);

    void delete(String key);
}
