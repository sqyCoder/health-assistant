package com.health.assistant.controller;

import com.health.assistant.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalFileController {

    private final FileService fileService;

    @GetMapping("/api/files/local/{bucket}/**")
    public void download(@PathVariable String bucket, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String uri = request.getRequestURI();
        String prefix = "/api/files/local/" + bucket + "/";
        String objectKey = URLDecoder.decode(uri.substring(prefix.length()), StandardCharsets.UTF_8);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        try (InputStream in = fileService.openStream(bucket, objectKey)) {
            StreamUtils.copy(in, response.getOutputStream());
        }
    }
}
