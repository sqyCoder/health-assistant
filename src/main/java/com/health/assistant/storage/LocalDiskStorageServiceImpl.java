package com.health.assistant.storage;

import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.common.exception.ThirdPartyException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public class LocalDiskStorageServiceImpl implements StorageService {

    private final Path rootDir;

    public LocalDiskStorageServiceImpl(StorageProperties.Local config) {
        this.rootDir = Paths.get(config.getRootDir()).toAbsolutePath().normalize();
    }

    @Override
    public void upload(String bucket, String objectKey, InputStream in, String contentType, long size) {
        Path target = resolve(bucket, objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "本地存储写入失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String bucket, String objectKey) {
        Path target = resolve(bucket, objectKey);
        try {
            return new ByteArrayInputStream(Files.readAllBytes(target));
        } catch (IOException e) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "本地存储读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String presignedUrl(String bucket, String objectKey, Duration ttl) {
        return "/api/files/local/" + bucket + "/" + objectKey;
    }

    @Override
    public void delete(String bucket, String objectKey) {
        Path target = resolve(bucket, objectKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "本地存储删除失败: " + e.getMessage(), e);
        }
    }

    private Path resolve(String bucket, String objectKey) {
        Path target = rootDir.resolve(bucket).resolve(objectKey).normalize();
        if (!target.startsWith(rootDir)) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "非法的对象路径: " + objectKey);
        }
        return target;
    }
}
