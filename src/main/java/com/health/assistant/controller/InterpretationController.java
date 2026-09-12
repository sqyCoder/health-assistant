package com.health.assistant.controller;

import com.health.assistant.common.api.Result;
import com.health.assistant.service.InterpretationService;
import com.health.assistant.vo.InterpretationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "AI 解读")
@RestController
@RequestMapping("/api/reports/{reportId}/interpretations")
@RequiredArgsConstructor
public class InterpretationController {

    private final InterpretationService interpretationService;

    @Operation(summary = "生成解读（异步任务）")
    @PostMapping
    public Result<Map<String, Long>> generate(@PathVariable Long reportId) {
        return Result.success(interpretationService.generate(reportId));
    }

    @Operation(summary = "最新解读结果")
    @GetMapping("/latest")
    public Result<InterpretationVO> getLatest(@PathVariable Long reportId) {
        return Result.success(interpretationService.getLatest(reportId));
    }
}
