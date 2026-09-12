package com.health.assistant.support;

import com.health.assistant.client.DeepSeekClient;
import com.health.assistant.client.DeepSeekProperties;
import com.health.assistant.entity.AiCallLog;
import com.health.assistant.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiLogRecorder {

    private final AiCallLogMapper aiCallLogMapper;
    private final DeepSeekProperties deepSeekProperties;

    public void recordSuccess(Long userId, Long taskId, String scene, DeepSeekClient.ChatResult result) {
        record(userId, taskId, scene, result.requestId(), result.promptTokens(), result.completionTokens(),
                result.durationMs(), 0);
    }

    private void record(Long userId, Long taskId, String scene, String requestId,
                        int promptTokens, int completionTokens, long durationMs, int isStream) {
        try {
            AiCallLog logEntry = new AiCallLog();
            logEntry.setUserId(userId);
            logEntry.setTaskId(taskId);
            logEntry.setScene(scene);
            logEntry.setModel(deepSeekProperties.getModel());
            logEntry.setRequestId(requestId);
            logEntry.setPromptTokens(promptTokens);
            logEntry.setCompletionTokens(completionTokens);
            logEntry.setTotalTokens(promptTokens + completionTokens);
            logEntry.setCostEstimate(estimateCost(promptTokens, completionTokens));
            logEntry.setDurationMs((int) durationMs);
            logEntry.setIsStream(isStream);
            logEntry.setStatus("SUCCESS");
            aiCallLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("failed to record ai call log", e);
        }
    }

    public void recordStreamSuccess(Long userId, Long taskId, String scene, DeepSeekClient.StreamResult result) {
        record(userId, taskId, scene, result.requestId(), result.promptTokens(), result.completionTokens(),
                result.durationMs(), 1);
    }

    public void recordFailure(Long userId, Long taskId, String scene, String errorCode, String errorMsg, long durationMs) {
        try {
            AiCallLog logEntry = new AiCallLog();
            logEntry.setUserId(userId);
            logEntry.setTaskId(taskId);
            logEntry.setScene(scene);
            logEntry.setModel(deepSeekProperties.getModel());
            logEntry.setCostEstimate(BigDecimal.ZERO);
            logEntry.setDurationMs((int) durationMs);
            logEntry.setIsStream(0);
            logEntry.setStatus("FAILED");
            logEntry.setErrorCode(errorCode);
            logEntry.setErrorMsg(errorMsg);
            aiCallLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("failed to record ai call log", e);
        }
    }

    private BigDecimal estimateCost(int promptTokens, int completionTokens) {
        double cost = promptTokens / 1_000_000.0 * deepSeekProperties.getPriceInputPerM()
                + completionTokens / 1_000_000.0 * deepSeekProperties.getPriceOutputPerM();
        return BigDecimal.valueOf(cost).setScale(6, RoundingMode.HALF_UP);
    }
}
