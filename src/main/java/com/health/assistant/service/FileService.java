package com.health.assistant.service;

import com.health.assistant.entity.FileObject;
import com.health.assistant.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;

public interface FileService {

    FileObject store(MultipartFile file, String bizType, Long memberId, String bucket);

    FileVO buildUrl(Long fileId, Long currentUserId, Duration ttl);

    InputStream openStream(String bucket, String objectKey);

    void delete(Long fileId, Long currentUserId);
}
