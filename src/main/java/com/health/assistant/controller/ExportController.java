package com.health.assistant.controller;

import com.health.assistant.common.api.Result;
import com.health.assistant.service.ExportService;
import com.health.assistant.vo.ExportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "PDF 导出")
@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "创建导出任务（INTERPRETATION/SUGGESTION）")
    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody Map<String, Object> body) {
        String exportType = body.get("exportType") == null ? null : body.get("exportType").toString();
        Long refId = body.get("refId") == null ? null : Long.valueOf(body.get("refId").toString());
        return Result.success(exportService.create(exportType, refId));
    }

    @Operation(summary = "导出记录详情")
    @GetMapping("/{id}")
    public Result<ExportVO> get(@PathVariable Long id) {
        return Result.success(exportService.get(id));
    }
}
