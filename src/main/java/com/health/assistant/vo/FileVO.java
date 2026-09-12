package com.health.assistant.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileVO {

    private final Long id;
    private final String originalName;
    private final Long fileSize;
    private final String mimeType;
    private final String url;
    private final long expiresInSeconds;
}
