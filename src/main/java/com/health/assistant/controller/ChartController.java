package com.health.assistant.controller;

import com.health.assistant.common.api.Result;
import com.health.assistant.service.ChartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "数据图表")
@RestController
@RequestMapping("/api/charts/{memberId}")
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;

    @Operation(summary = "首页概览数据")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@PathVariable Long memberId) {
        return Result.success(chartService.overview(memberId));
    }

    @Operation(summary = "指标趋势（按指标编码）")
    @GetMapping("/indicator-trend")
    public Result<List<Map<String, Object>>> indicatorTrend(
            @PathVariable Long memberId,
            @RequestParam String metricCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        return Result.success(chartService.indicatorTrend(memberId, metricCode, from, to));
    }

    @Operation(summary = "运动量趋势（day/week）")
    @GetMapping("/exercise-trend")
    public Result<List<Map<String, Object>>> exerciseTrend(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        return Result.success(chartService.exerciseTrend(memberId, granularity, from, to));
    }

    @Operation(summary = "运动类型分布")
    @GetMapping("/workout-distribution")
    public Result<List<Map<String, Object>>> workoutDistribution(
            @PathVariable Long memberId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        return Result.success(chartService.workoutDistribution(memberId, from, to));
    }

    @Operation(summary = "体征趋势（weight/bp/glucose）")
    @GetMapping("/vital-trend")
    public Result<List<Map<String, Object>>> vitalTrend(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "weight") String type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        return Result.success(chartService.vitalTrend(memberId, type, from, to));
    }
}
