package com.health.assistant.controller;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.api.Result;
import com.health.assistant.service.MetricService;
import com.health.assistant.vo.MetricCategoryVO;
import com.health.assistant.vo.MetricDetailVO;
import com.health.assistant.vo.MetricVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "指标字典")
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @Operation(summary = "指标分类列表")
    @GetMapping("/categories")
    public Result<List<MetricCategoryVO>> listCategories() {
        return Result.success(metricService.listCategories());
    }

    @Operation(summary = "指标搜索（分页）")
    @GetMapping
    public Result<PageResult<MetricVO>> search(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(metricService.search(keyword, categoryId, pageNum, pageSize));
    }

    @Operation(summary = "指标详情（含参考范围与说明）")
    @GetMapping("/{id}")
    public Result<MetricDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(metricService.getDetail(id));
    }
}
