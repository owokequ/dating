package com.dating.owoke.places.sync.dto;

public record SyncResponse(int received, int created, int updated, int unchanged, int duplicates) {
}
