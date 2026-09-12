package com.health.assistant.service.impl;

import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.common.exception.ThirdPartyException;
import com.health.assistant.common.util.HashUtils;
import com.health.assistant.entity.FileObject;
import com.health.assistant.mapper.FileObjectMapper;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.FileService;
import com.health.assistant.storage.StorageProperties;
import com.health.assistant.storage.StorageService;
import com.health.assistant.vo.FileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final int MAX_EXTENSION_LENGTH = 10;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final FileObjectMapper fileObjectMapper;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileObject store(MultipartFile file, String bizType, Long memberId, String bucket) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "unnamed";
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "读取上传文件失败");
        }
        String objectKey = buildObjectKey(bizType, userId, memberId, originalName);
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            storageService.upload(bucket, objectKey, in, file.getContentType(), bytes.length);
        } catch (IOException e) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "上传文件失败: " + e.getMessage(), e);
        }

        FileObject fileObject = new FileObject();
        fileObject.setUserId(userId);
        fileObject.setMemberId(memberId);
        fileObject.setBizType(bizType);
        fileObject.setOriginalName(originalName);
        fileObject.setStorageType(storageProperties.getType());
        fileObject.setBucket(bucket);
        fileObject.setObjectKey(objectKey);
        fileObject.setFileSize((long) bytes.length);
        fileObject.setMimeType(file.getContentType());
        fileObject.setSha256(HashUtils.sha256Hex(bytes));
        fileObject.setStatus(STATUS_ACTIVE);
        fileObjectMapper.insert(fileObject);
        return fileObject;
    }

    @Override
    public FileVO buildUrl(Long fileId, Long currentUserId, Duration ttl) {
        FileObject fileObject = getOwnedFile(fileId, currentUserId);
        String url = storageService.presignedUrl(fileObject.getBucket(), fileObject.getObjectKey(), ttl);
        return FileVO.builder()
                .id(fileObject.getId())
                .originalName(fileObject.getOriginalName())
                .fileSize(fileObject.getFileSize())
                .mimeType(fileObject.getMimeType())
                .url(url)
                .expiresInSeconds(ttl.getSeconds())
                .build();
    }

    @Override
    public InputStream openStream(String bucket, String objectKey) {
        return storageService.download(bucket, objectKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long fileId, Long currentUserId) {
        FileObject fileObject = getOwnedFile(fileId, currentUserId);
        storageService.delete(fileObject.getBucket(), fileObject.getObjectKey());
        fileObject.setStatus(STATUS_DELETED);
        fileObjectMapper.updateById(fileObject);
        fileObjectMapper.deleteById(fileObject.getId());
    }

    private FileObject getOwnedFile(Long fileId, Long currentUserId) {
        FileObject fileObject = fileObjectMapper.selectById(fileId);
        if (fileObject == null || !STATUS_ACTIVE.equals(fileObject.getStatus())) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        if (!fileObject.getUserId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        return fileObject;
    }

    private String buildObjectKey(String bizType, Long userId, Long memberId, String originalName) {
        String prefix = StringUtils.hasText(bizType) ? bizType.toLowerCase(Locale.ROOT) : "other";
        String month = LocalDate.now().format(MONTH_FORMAT);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + "/" + userId + "/" + (memberId == null ? 0 : memberId) + "/" + month + "/"
                + uuid + extensionOf(originalName);
    }

    private String extensionOf(String originalName) {
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        String extension = originalName.substring(dot + 1).replaceAll("[^a-zA-Z0-9]", "");
        if (extension.isEmpty() || extension.length() > MAX_EXTENSION_LENGTH) {
            return "";
        }
        return "." + extension.toLowerCase(Locale.ROOT);
    }
}
