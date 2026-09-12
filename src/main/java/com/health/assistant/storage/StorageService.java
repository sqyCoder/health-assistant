package com.health.assistant.storage;

import java.io.InputStream;
import java.time.Duration;

public interface StorageService {

    void upload(String bucket, String objectKey, InputStream in, String contentType, long size);

    InputStream download(String bucket, String objectKey);

    String presignedUrl(String bucket, String objectKey, Duration ttl);

    void delete(String bucket, String objectKey);
}
