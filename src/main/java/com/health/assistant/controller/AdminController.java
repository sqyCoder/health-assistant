package com.health.assistant.controller;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.api.Result;
import com.health.assistant.dto.admin.MetricRangeItem;
import com.health.assistant.dto.admin.MetricUpsertRequest;
import com.health.assistant.dto.admin.PromptUpsertRequest;
import com.health.assistant.entity.SysPromptTemplate;
import com.health.assistant.service.AdminService;
import com.health.assistant.service.MetricService;
import com.health.assistant.vo.AdminAiLogVO;
import com.health.assistant.vo.MetricDetailVO;
import com.health.assistant.vo.MetricVO;
import com.health.assistant.vo.TaskVO;
import com.health.assistant.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "管理后台")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final MetricService metricService;

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<PageResult<UserVO>> listUsers(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(adminService.listUsers(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.updateUserStatus(id, body.get("status"));
        return Result.success();
    }

    @Operation(summary = "重置用户密码（返回临时密码）")
    @PutMapping("/users/{id}/reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id) {
        return Result.success(Map.of("tempPassword", adminService.resetPassword(id)));
    }

    @Operation(summary = "指标字典搜索")
    @GetMapping("/metrics")
    public Result<PageResult<MetricVO>> searchMetrics(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long categoryId,
                                                      @RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(metricService.search(keyword, categoryId, pageNum, pageSize));
    }

    @Operation(summary = "新增指标")
    @PostMapping("/metrics")
    public Result<MetricDetailVO> createMetric(@Valid @RequestBody MetricUpsertRequest request) {
        return Result.success(metricService.adminCreate(request));
    }

    @Operation(summary = "修改指标")
    @PutMapping("/metrics/{id}")
    public Result<MetricDetailVO> updateMetric(@PathVariable Long id,
                                               @Valid @RequestBody MetricUpsertRequest request) {
        return Result.success(metricService.adminUpdate(id, request));
    }

    @Operation(summary = "启用/停用指标")
    @PutMapping("/metrics/{id}/status")
    public Result<Void> updateMetricStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        metricService.adminUpdateStatus(id, body.get("status"));
        return Result.success();
    }

    @Operation(summary = "全量替换指标参考范围")
    @PutMapping("/metrics/{id}/ranges")
    public Result<MetricDetailVO> replaceRanges(@PathVariable Long id,
                                                @RequestBody Map<String, List<MetricRangeItem>> body) {
        return Result.success(metricService.adminReplaceRanges(id, body.get("ranges")));
    }

    @Operation(summary = "Prompt 模板列表")
    @GetMapping("/prompts")
    public Result<List<SysPromptTemplate>> listPrompts(@RequestParam(required = false) String code) {
        return Result.success(adminService.listPrompts(code));
    }

    @Operation(summary = "新建 Prompt 版本（自动启用并停用旧版本）")
    @PostMapping("/prompts")
    public Result<SysPromptTemplate> createPrompt(@Valid @RequestBody PromptUpsertRequest request) {
        return Result.success(adminService.createPromptVersion(request));
    }

    @Operation(summary = "启用/停用 Prompt 模板")
    @PutMapping("/prompts/{id}/status")
    public Result<Void> updatePromptStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.updatePromptStatus(id, body.get("status"));
        return Result.success();
    }

    @Operation(summary = "AI 调用日志")
    @GetMapping("/ai-logs")
    public Result<PageResult<AdminAiLogVO>> listAiLogs(
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(adminService.listAiLogs(scene, userId, dateFrom, dateTo, pageNum, pageSize));
    }

    @Operation(summary = "AI 用量按天汇总")
    @GetMapping("/ai-logs/summary")
    public Result<List<Map<String, Object>>> aiLogSummary(@RequestParam(defaultValue = "30") int days) {
        return Result.success(adminService.aiLogSummary(days));
    }

    @Operation(summary = "异步任务列表")
    @GetMapping("/tasks")
    public Result<PageResult<TaskVO>> listTasks(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String taskType,
                                                @RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(adminService.listTasks(status, taskType, pageNum, pageSize));
    }

    @Operation(summary = "重试任务")
    @PostMapping("/tasks/{id}/retry")
    public Result<Void> retryTask(@PathVariable Long id) {
        adminService.retryTask(id);
        return Result.success();
    }
}
