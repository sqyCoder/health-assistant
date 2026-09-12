package com.health.assistant.controller;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.api.Result;
import com.health.assistant.dto.report.IndicatorReviewRequest;
import com.health.assistant.dto.report.ReportManualRequest;
import com.health.assistant.dto.report.ReportMetaUpdateRequest;
import com.health.assistant.service.ReportService;
import com.health.assistant.vo.ReportDetailVO;
import com.health.assistant.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Tag(name = "体检报告")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "上传 PDF 体检报告")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ReportVO> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam Long memberId,
                                   @RequestParam(required = false) String reportName,
                                   @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reportDate,
                                   @RequestParam(required = false) String institution,
                                   @RequestParam(defaultValue = "ANNUAL") String reportType) {
        return Result.success(reportService.upload(file, memberId, reportName, reportDate, institution, reportType));
    }

    @Operation(summary = "手动创建报告（指标直接录入）")
    @PostMapping("/manual")
    public Result<ReportDetailVO> createManual(@Valid @RequestBody ReportManualRequest request) {
        return Result.success(reportService.createManual(request));
    }

    @Operation(summary = "报告分页列表")
    @GetMapping
    public Result<PageResult<ReportVO>> list(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(reportService.list(memberId, reportType, dateFrom, dateTo, pageNum, pageSize));
    }

    @Operation(summary = "报告详情（含指标明细）")
    @GetMapping("/{id}")
    public Result<ReportDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(reportService.getDetail(id));
    }

    @Operation(summary = "修改报告元信息")
    @PutMapping("/{id}")
    public Result<ReportVO> updateMeta(@PathVariable Long id, @Valid @RequestBody ReportMetaUpdateRequest request) {
        return Result.success(reportService.updateMeta(id, request));
    }

    @Operation(summary = "校对保存指标（全量替换，重新判定）")
    @PutMapping("/{id}/indicators")
    public Result<ReportDetailVO> updateIndicators(@PathVariable Long id,
                                                   @Valid @RequestBody IndicatorReviewRequest request) {
        return Result.success(reportService.updateIndicators(id, request));
    }

    @Operation(summary = "重新解析 PDF 报告（异步任务）")
    @PostMapping("/{id}/parse")
    public Result<Long> parse(@PathVariable Long id) {
        return Result.success(reportService.parse(id));
    }

    @Operation(summary = "删除报告（含指标与文件）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return Result.success();
    }
}
