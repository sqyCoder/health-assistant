package com.health.assistant.client;

import okhttp3.Call;

public interface DeepSeekClient {

    ChatResult chat(ChatRequest request);

    Call streamChat(ChatRequest request, StreamListener listener);

    record ChatRequest(String systemPrompt, String userPrompt, double temperature, int maxTokens, boolean jsonMode) {
    }

    record ChatResult(String content, String requestId, int promptTokens, int completionTokens, long durationMs) {
    }

    record StreamResult(int promptTokens, int completionTokens, String requestId, long durationMs) {
    }

    interface StreamListener {

        void onDelta(String delta);

        void onComplete(StreamResult result);

        void onError(Throwable error);
    }
}
