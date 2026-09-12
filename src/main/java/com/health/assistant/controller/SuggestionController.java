package com.health.assistant.controller;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.api.Result;
import com.health.assistant.dto.suggestion.SuggestionGenerateRequest;
import com.health.assistant.service.SuggestionService;
import com.health.assistant.vo.SuggestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "锻炼与康复建议")
@RestController
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    @Operation(summary = "生成建议（异步任务）")
    @PostMapping("/api/members/{memberId}/suggestions")
    public Result<Map<String, Long>> generate(@PathVariable Long memberId,
                                              @Valid @RequestBody SuggestionGenerateRequest request) {
        return Result.success(suggestionService.generate(memberId, request));
    }

    @Operation(summary = "建议历史列表")
    @GetMapping("/api/members/{memberId}/suggestions")
    public Result<PageResult<SuggestionVO>> list(@PathVariable Long memberId,
                                                 @RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(suggestionService.list(memberId, pageNum, pageSize));
    }

    @Operation(summary = "建议详情")
    @GetMapping("/api/suggestions/{id}")
    public Result<SuggestionVO> get(@PathVariable Long id) {
        return Result.success(suggestionService.get(id));
    }

    @Operation(summary = "删除建议")
    @DeleteMapping("/api/suggestions/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        suggestionService.delete(id);
        return Result.success();
    }
}
