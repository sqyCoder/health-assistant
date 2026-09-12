package com.health.assistant.service;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.dto.suggestion.SuggestionGenerateRequest;
import com.health.assistant.vo.SuggestionVO;

import java.util.Map;

public interface SuggestionService {

    Map<String, Long> generate(Long memberId, SuggestionGenerateRequest request);

    PageResult<SuggestionVO> list(Long memberId, long pageNum, long pageSize);

    SuggestionVO get(Long id);

    void delete(Long id);
}
