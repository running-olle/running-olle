package com.runningolle.domain.community.storage;

public record StoredFile(
        byte[] content,
        String contentType
) {
}