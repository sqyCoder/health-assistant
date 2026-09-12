package com.health.assistant.controller;

import com.health.assistant.common.api.Result;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.FileObject;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.FileService;
import com.health.assistant.storage.StorageProperties;
import com.health.assistant.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final Duration URL_TTL = Duration.ofMinutes(60);
    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of("REPORT", "AVATAR", "OTHER");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/webp");

    private final FileService fileService;
    private final StorageProperties storageProperties;

    @Operation(summary = "上传文件（PDF/图片，最大 20MB）")
    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(defaultValue = "REPORT") String bizType,
                                 @RequestParam(required = false) Long memberId) {
        String normalizedBizType = bizType.toUpperCase(Locale.ROOT);
        if (!ALLOWED_BIZ_TYPES.contains(normalizedBizType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的业务类型: " + bizType);
        }
        if (file.getContentType() == null || !ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        FileObject stored = fileService.store(file, normalizedBizType, memberId, resolveBucket(normalizedBizType));
        return Result.success(FileVO.builder()
                .id(stored.getId())
                .originalName(stored.getOriginalName())
                .fileSize(stored.getFileSize())
                .mimeType(stored.getMimeType())
                .expiresInSeconds(0)
                .build());
    }

    @Operation(summary = "获取文件访问地址（签名 URL，60 分钟有效）")
    @GetMapping("/{id}/url")
    public Result<FileVO> getUrl(@PathVariable Long id) {
        return Result.success(fileService.buildUrl(id, SecurityUtils.getCurrentUserId(), URL_TTL));
    }

    private String resolveBucket(String bizType) {
        return "EXPORT".equals(bizType)
                ? storageProperties.getSupabase().getBucketExports()
                : storageProperties.getSupabase().getBucketReports();
    }
}
