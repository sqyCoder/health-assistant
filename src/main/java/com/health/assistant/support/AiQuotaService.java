package com.health.assistant.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.AiCallLog;
import com.health.assistant.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AiQuotaService {

    private final AiCallLogMapper aiCallLogMapper;

    @Value("${app.ai-daily-limit-per-user:3}")
    private int dailyLimit;

    public void checkQuota(Long userId) {
        if (dailyLimit <= 0) {
            return;
        }
        Long used = aiCallLogMapper.selectCount(Wrappers.<AiCallLog>lambdaQuery()
                .eq(AiCallLog::getUserId, userId)
                .ge(AiCallLog::getCreatedAt, LocalDate.now().atStartOfDay()));
        if (used != null && used >= dailyLimit) {
            throw new BizException(ErrorCode.AI_DAILY_LIMIT_EXCEEDED,
                    "今日 AI 次数已用完（每人每天 " + dailyLimit + " 次），明天再来吧");
        }
    }
}
