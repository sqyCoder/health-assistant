package com.health.assistant.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.common.exception.ThirdPartyException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SupabaseRestStorageServiceImpl implements StorageService {

    private static final String STORAGE_PATH = "/storage/v1";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final StorageProperties.Supabase config;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final String baseUrl;

    public SupabaseRestStorageServiceImpl(StorageProperties.Supabase config, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(config.getUrl()) || !StringUtils.hasText(config.getServiceRoleKey())) {
            throw new IllegalStateException("storage.supabase.url or storage.supabase.service-role-key is not configured");
        }
        this.config = config;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(config.getUrl()) + STORAGE_PATH;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void upload(String bucket, String objectKey, InputStream in, String contentType, long size) {
        MediaType mediaType = StringUtils.hasText(contentType) ? MediaType.parse(contentType) : null;
        RequestBody body = new RequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public long contentLength() {
                return size;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.writeAll(Okio.source(in));
            }
        };
        Request request = baseRequest(url("object", bucket, objectKey))
                .post(body)
                .build();
        execute(request, "上传文件失败");
    }

    @Override
    public InputStream download(String bucket, String objectKey) {
        Request request = baseRequest(url("object", bucket, objectKey)).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw buildError("下载文件失败", response);
            }
            return new ByteArrayInputStream(response.body().bytes());
        } catch (IOException e) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "下载文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String presignedUrl(String bucket, String objectKey, Duration ttl) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("expiresIn", ttl.getSeconds()));
        } catch (IOException e) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "生成签名请求失败", e);
        }
        Request request = baseRequest(url("object", "sign", bucket, objectKey))
                .post(RequestBody.create(payload, JSON_MEDIA_TYPE))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw buildError("生成文件签名链接失败", response);
            }
            JsonNode root = objectMapper.readTree(response.body().bytes());
            String signedUrl = root.path("signedURL").asText(null);
            if (!StringUtils.hasText(signedUrl)) {
                throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "生成文件签名链接失败: 响应缺少 signedURL");
            }
            return baseUrl + signedUrl;
        } catch (IOException e) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "生成文件签名链接失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        Request request = baseRequest(url("object", bucket, objectKey)).delete().build();
        execute(request, "删除文件失败");
    }

    private void execute(Request request, String errorMessage) {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw buildError(errorMessage, response);
            }
        } catch (IOException e) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, errorMessage + ": " + e.getMessage(), e);
        }
    }

    private ThirdPartyException buildError(String errorMessage, Response response) {
        String detail = "";
        try {
            if (response.body() != null) {
                String body = response.body().string();
                JsonNode node = objectMapper.readTree(body);
                detail = node.path("message").asText(body);
            }
        } catch (IOException ignored) {
            detail = "";
        }
        return new ThirdPartyException(ErrorCode.STORAGE_ERROR,
                errorMessage + " (HTTP " + response.code() + "): " + detail);
    }

    private Request.Builder baseRequest(String url) {
        return new Request.Builder()
                .url(url)
                .header("apikey", config.getServiceRoleKey())
                .header("Authorization", "Bearer " + config.getServiceRoleKey());
    }

    private String url(String... segments) {
        HttpUrl httpUrl = HttpUrl.parse(baseUrl);
        if (httpUrl == null) {
            throw new ThirdPartyException(ErrorCode.STORAGE_ERROR, "非法的存储服务地址: " + baseUrl);
        }
        HttpUrl.Builder builder = httpUrl.newBuilder();
        for (String segment : segments) {
            builder.addPathSegments(segment);
        }
        return builder.build().toString();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
