package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.SysPromptTemplate;
import com.health.assistant.mapper.SysPromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final SysPromptTemplateMapper templateMapper;

    public record RenderedPrompt(String content, int version, double temperature, int maxTokens) {
    }

    public RenderedPrompt render(String code, Map<String, String> variables) {
        SysPromptTemplate template = templateMapper.selectOne(Wrappers.<SysPromptTemplate>lambdaQuery()
                .eq(SysPromptTemplate::getCode, code)
                .eq(SysPromptTemplate::getStatus, "ENABLED")
                .orderByDesc(SysPromptTemplate::getVersion)
                .last("LIMIT 1"));
        if (template == null) {
            throw new BizException(ErrorCode.AI_GENERATE_FAILED, "Prompt 模板未配置: " + code);
        }
        String content = template.getContent();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}",
                        entry.getValue() == null ? "" : entry.getValue());
            }
        }
        double temperature = template.getTemperature() == null ? 0.3 : template.getTemperature().doubleValue();
        int maxTokens = template.getMaxTokens() == null ? 4096 : template.getMaxTokens();
        return new RenderedPrompt(content, template.getVersion(), temperature, maxTokens);
    }
}
