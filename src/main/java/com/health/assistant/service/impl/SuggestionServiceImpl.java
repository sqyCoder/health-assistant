package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.dto.suggestion.SuggestionGenerateRequest;
import com.health.assistant.entity.AiSuggestion;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.mapper.AiSuggestionMapper;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.SuggestionService;
import com.health.assistant.support.AiQuotaService;
import com.health.assistant.support.MemberAccessChecker;
import com.health.assistant.task.AiSuggestionTaskHandler;
import com.health.assistant.vo.SuggestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {

    private static final Set<String> SUGGESTION_TYPES = Set.of("EXERCISE", "REHAB", "COMPREHENSIVE");
    private static final Set<String> SOURCE_TYPES = Set.of("REPORT", "EXERCISE", "INTERPRETATION", "MANUAL");

    private final AiSuggestionMapper suggestionMapper;
    private final MemberAccessChecker memberAccessChecker;
    private final AsyncTaskService asyncTaskService;
    private final AiQuotaService aiQuotaService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> generate(Long memberId, SuggestionGenerateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        aiQuotaService.checkQuota(userId);
        if (!SUGGESTION_TYPES.contains(request.getSuggestionType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "建议类型取值非法: " + request.getSuggestionType());
        }
        String sourceType = StringUtils.hasText(request.getSourceType()) ? request.getSourceType() : "MANUAL";
        if (!SOURCE_TYPES.contains(sourceType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "来源类型取值非法: " + sourceType);
        }

        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setMemberId(member.getId());
        suggestion.setUserId(userId);
        suggestion.setSuggestionType(request.getSuggestionType());
        suggestion.setSourceType(sourceType);
        suggestion.setSourceId(request.getSourceId());
        suggestion.setGoal(request.getGoal());
        suggestion.setStatus("PROCESSING");
        suggestionMapper.insert(suggestion);

        Long taskId = asyncTaskService.submit(AiSuggestionTaskHandler.TASK_TYPE, userId,
                member.getId(), "SUGGESTION", suggestion.getId(), null);

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("suggestionId", suggestion.getId());
        result.put("taskId", taskId);
        return result;
    }

    @Override
    public PageResult<SuggestionVO> list(Long memberId, long pageNum, long pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        memberAccessChecker.check(userId, memberId);
        Page<AiSuggestion> page = suggestionMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<AiSuggestion>lambdaQuery()
                        .eq(AiSuggestion::getMemberId, memberId)
                        .orderByDesc(AiSuggestion::getId));
        return PageResult.of(page, SuggestionVO::from);
    }

    @Override
    public SuggestionVO get(Long id) {
        return SuggestionVO.from(getOwned(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getOwned(id);
        suggestionMapper.deleteById(id);
    }

    private AiSuggestion getOwned(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        AiSuggestion suggestion = suggestionMapper.selectById(id);
        if (suggestion == null || !suggestion.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "建议不存在或无权访问");
        }
        return suggestion;
    }
}
