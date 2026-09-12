package com.health.assistant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.common.exception.ThirdPartyException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DeepSeekClientImpl implements DeepSeekClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_ATTEMPTS = 3;

    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public DeepSeekClientImpl(DeepSeekProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(properties.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new ThirdPartyException(ErrorCode.DEEPSEEK_ERROR, "未配置 DeepSeek API Key");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", request.systemPrompt()),
                Map.of("role", "user", "content", request.userPrompt())));
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());
        if (request.jsonMode()) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new ThirdPartyException(ErrorCode.DEEPSEEK_ERROR, "请求序列化失败", e);
        }
        Request httpRequest = new Request.Builder()
                .url(trimTrailingSlash(properties.getBaseUrl()) + "/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                .build();

        long start = System.currentTimeMillis();
        IOException lastIoException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                if (response.isSuccessful()) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    String content = root.path("choices").path(0).path("message").path("content").asText("");
                    String requestId = root.path("id").asText(null);
                    int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
                    int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
                    return new ChatResult(content, requestId, promptTokens, completionTokens,
                            System.currentTimeMillis() - start);
                }
                if (response.code() == 429 || response.code() >= 500) {
                    log.warn("deepseek retryable error: status={}, attempt={}", response.code(), attempt);
                    if (attempt < MAX_ATTEMPTS) {
                        sleep(1000L * attempt);
                        continue;
                    }
                }
                ErrorCode errorCode = response.code() == 429
                        ? ErrorCode.DEEPSEEK_RATE_LIMITED
                        : ErrorCode.DEEPSEEK_ERROR;
                throw new ThirdPartyException(errorCode,
                        "AI 调用失败 (HTTP " + response.code() + "): " + truncate(responseBody, 300));
            } catch (IOException e) {
                lastIoException = e;
                log.warn("deepseek io error: attempt={}, message={}", attempt, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep(1000L * attempt);
                    continue;
                }
            }
        }
        throw new ThirdPartyException(ErrorCode.DEEPSEEK_TIMEOUT,
                "AI 服务连接失败: " + (lastIoException == null ? "unknown" : lastIoException.getMessage()),
                lastIoException);
    }

    @Override
    public Call streamChat(ChatRequest request, StreamListener listener) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            listener.onError(new ThirdPartyException(ErrorCode.DEEPSEEK_ERROR, "未配置 DeepSeek API Key"));
            return null;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", request.systemPrompt()),
                Map.of("role", "user", "content", request.userPrompt())));
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());
        body.put("stream", true);
        body.put("stream_options", Map.of("include_usage", true));
        if (request.jsonMode()) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            listener.onError(new ThirdPartyException(ErrorCode.DEEPSEEK_ERROR, "请求序列化失败", e));
            return null;
        }
        Request httpRequest = new Request.Builder()
                .url(trimTrailingSlash(properties.getBaseUrl()) + "/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Accept", "text/event-stream")
                .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                .build();

        long start = System.currentTimeMillis();
        Call call = httpClient.newCall(httpRequest);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call failedCall, IOException e) {
                listener.onError(e);
            }

            @Override
            public void onResponse(Call called, Response response) {
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        String detail = response.body() == null ? "" : truncate(response.body().string(), 300);
                        listener.onError(new ThirdPartyException(ErrorCode.DEEPSEEK_ERROR,
                                "AI 流式调用失败 (HTTP " + response.code() + "): " + detail));
                        return;
                    }
                    int promptTokens = 0;
                    int completionTokens = 0;
                    String requestId = null;
                    okio.BufferedSource source = response.body().source();
                    String line;
                    while ((line = source.readUtf8Line()) != null) {
                        if (line.isEmpty() || line.startsWith(":")) {
                            continue;
                        }
                        if (!line.startsWith("data:")) {
                            continue;
                        }
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        JsonNode node = objectMapper.readTree(data);
                        if (node.hasNonNull("id")) {
                            requestId = node.get("id").asText();
                        }
                        JsonNode deltaNode = node.path("choices").path(0).path("delta").path("content");
                        if (!deltaNode.isMissingNode() && !deltaNode.isNull()) {
                            String text = deltaNode.asText();
                            if (!text.isEmpty()) {
                                listener.onDelta(text);
                            }
                        }
                        JsonNode usage = node.path("usage");
                        if (!usage.isMissingNode() && !usage.isNull()) {
                            promptTokens = usage.path("prompt_tokens").asInt(promptTokens);
                            completionTokens = usage.path("completion_tokens").asInt(completionTokens);
                        }
                    }
                    listener.onComplete(new StreamResult(promptTokens, completionTokens, requestId,
                            System.currentTimeMillis() - start));
                } catch (Exception e) {
                    listener.onError(e);
                }
            }
        });
        return call;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThirdPartyException(ErrorCode.DEEPSEEK_ERROR, "AI 调用被中断", e);
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
