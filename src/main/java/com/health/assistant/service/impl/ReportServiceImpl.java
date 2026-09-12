package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.constant.ReportConstants;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.dto.report.IndicatorItemRequest;
import com.health.assistant.dto.report.IndicatorReviewRequest;
import com.health.assistant.dto.report.ReportManualRequest;
import com.health.assistant.dto.report.ReportMetaUpdateRequest;
import com.health.assistant.entity.ExamIndicator;
import com.health.assistant.entity.ExamReport;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.entity.FileObject;
import com.health.assistant.entity.MetricCategory;
import com.health.assistant.entity.MetricDict;
import com.health.assistant.entity.MetricReferenceRange;
import com.health.assistant.mapper.ExamIndicatorMapper;
import com.health.assistant.mapper.ExamReportMapper;
import com.health.assistant.mapper.MetricCategoryMapper;
import com.health.assistant.mapper.MetricDictMapper;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.FileService;
import com.health.assistant.service.ReportService;
import com.health.assistant.storage.StorageProperties;
import com.health.assistant.support.IndicatorAssembler;
import com.health.assistant.support.MemberAccessChecker;
import com.health.assistant.support.MetricRangeResolver;
import com.health.assistant.support.RangeEvaluator;
import com.health.assistant.task.ReportParseTaskHandler;
import com.health.assistant.vo.IndicatorVO;
import com.health.assistant.vo.ReportDetailVO;
import com.health.assistant.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "^(-?\\d+(?:\\.\\d+)?)\\s*[-~至]\\s*(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern RANGE_MAX_PATTERN = Pattern.compile(
            "^[<≤]\\s*(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern RANGE_MIN_PATTERN = Pattern.compile(
            "^[>≥]\\s*(-?\\d+(?:\\.\\d+)?)$");

    private final ExamReportMapper reportMapper;
    private final ExamIndicatorMapper indicatorMapper;
    private final MetricDictMapper metricDictMapper;
    private final MetricCategoryMapper metricCategoryMapper;
    private final MemberAccessChecker memberAccessChecker;
    private final MetricRangeResolver rangeResolver;
    private final RangeEvaluator rangeEvaluator;
    private final IndicatorAssembler indicatorAssembler;
    private final AsyncTaskService asyncTaskService;
    private final FileService fileService;
    private final StorageProperties storageProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportVO upload(MultipartFile file, Long memberId, String reportName, LocalDate reportDate,
                           String institution, String reportType) {
        Long userId = SecurityUtils.getCurrentUserId();
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        validateReportType(reportType);
        if (reportDate == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "体检日期不能为空");
        }
        FileObject fileObject = fileService.store(file, "REPORT", memberId,
                storageProperties.getSupabase().getBucketReports());

        ExamReport report = new ExamReport();
        report.setMemberId(memberId);
        report.setUserId(userId);
        report.setReportName(defaultReportName(reportName, member));
        report.setReportDate(reportDate);
        report.setInstitution(institution);
        report.setReportType(reportType);
        report.setSourceType(ReportConstants.SOURCE_PDF);
        report.setFileId(fileObject.getId());
        report.setParseStatus(ReportConstants.PARSE_UPLOADED);
        report.setIndicatorCount(0);
        report.setAbnormalCount(0);
        report.setAiStatus(ReportConstants.AI_STATUS_NONE);
        reportMapper.insert(report);
        try {
            asyncTaskService.submit(ReportParseTaskHandler.TASK_TYPE, userId, memberId,
                    "REPORT", report.getId(), null);
        } catch (Exception e) {
            log.warn("failed to submit parse task for report {}", report.getId(), e);
        }
        return ReportVO.from(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportDetailVO createManual(ReportManualRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        FamilyMember member = memberAccessChecker.check(userId, request.getMemberId());
        validateReportType(request.getReportType());

        ExamReport report = new ExamReport();
        report.setMemberId(member.getId());
        report.setUserId(userId);
        report.setReportName(defaultReportName(request.getReportName(), member));
        report.setReportDate(request.getReportDate());
        report.setInstitution(request.getInstitution());
        report.setReportType(request.getReportType());
        report.setSourceType(ReportConstants.SOURCE_MANUAL);
        report.setParseStatus(ReportConstants.PARSE_CONFIRMED);
        report.setIndicatorCount(0);
        report.setAbnormalCount(0);
        report.setAiStatus(ReportConstants.AI_STATUS_NONE);
        report.setRemark(request.getRemark());
        reportMapper.insert(report);

        List<ExamIndicator> indicators = buildIndicators(report, member, request.getIndicators(), "MANUAL");
        for (ExamIndicator indicator : indicators) {
            indicatorMapper.insert(indicator);
        }
        report.setIndicatorCount(indicators.size());
        report.setAbnormalCount(countAbnormal(indicators));
        reportMapper.updateById(report);
        return detail(report, indicators);
    }

    @Override
    public PageResult<ReportVO> list(Long memberId, String reportType, LocalDate dateFrom, LocalDate dateTo,
                                     long pageNum, long pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (memberId != null) {
            memberAccessChecker.check(userId, memberId);
        }
        LambdaQueryWrapper<ExamReport> wrapper = Wrappers.<ExamReport>lambdaQuery()
                .eq(ExamReport::getUserId, userId)
                .eq(memberId != null, ExamReport::getMemberId, memberId)
                .eq(StringUtils.hasText(reportType), ExamReport::getReportType, reportType)
                .ge(dateFrom != null, ExamReport::getReportDate, dateFrom)
                .le(dateTo != null, ExamReport::getReportDate, dateTo)
                .orderByDesc(ExamReport::getReportDate)
                .orderByDesc(ExamReport::getId);
        Page<ExamReport> page = reportMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page, ReportVO::from);
    }

    @Override
    public ReportDetailVO getDetail(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        ExamReport report = getOwnedReport(userId, id);
        List<ExamIndicator> indicators = indicatorMapper.selectList(Wrappers.<ExamIndicator>lambdaQuery()
                .eq(ExamIndicator::getReportId, id)
                .orderByAsc(ExamIndicator::getSort)
                .orderByAsc(ExamIndicator::getId));
        return detail(report, indicators);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportVO updateMeta(Long id, ReportMetaUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ExamReport report = getOwnedReport(userId, id);
        validateReportType(request.getReportType());
        report.setReportName(request.getReportName());
        report.setReportDate(request.getReportDate());
        report.setInstitution(request.getInstitution());
        report.setReportType(request.getReportType());
        report.setRemark(request.getRemark());
        reportMapper.updateById(report);
        return ReportVO.from(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportDetailVO updateIndicators(Long id, IndicatorReviewRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ExamReport report = getOwnedReport(userId, id);
        FamilyMember member = memberAccessChecker.check(userId, report.getMemberId());

        indicatorMapper.delete(Wrappers.<ExamIndicator>lambdaQuery().eq(ExamIndicator::getReportId, id));
        List<ExamIndicator> indicators = buildIndicators(report, member, request.getIndicators(), "MANUAL");
        for (ExamIndicator indicator : indicators) {
            indicatorMapper.insert(indicator);
        }
        report.setIndicatorCount(indicators.size());
        report.setAbnormalCount(countAbnormal(indicators));
        report.setParseStatus(ReportConstants.PARSE_CONFIRMED);
        reportMapper.updateById(report);
        return detail(report, indicators);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long parse(Long reportId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ExamReport report = getOwnedReport(userId, reportId);
        if (!ReportConstants.SOURCE_PDF.equals(report.getSourceType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仅 PDF 报告支持解析");
        }
        report.setParseStatus(ReportConstants.PARSE_UPLOADED);
        reportMapper.updateById(report);
        return asyncTaskService.submit(ReportParseTaskHandler.TASK_TYPE, userId, report.getMemberId(),
                "REPORT", report.getId(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        ExamReport report = getOwnedReport(userId, id);
        indicatorMapper.delete(Wrappers.<ExamIndicator>lambdaQuery().eq(ExamIndicator::getReportId, id));
        reportMapper.deleteById(id);
        if (report.getFileId() != null) {
            try {
                fileService.delete(report.getFileId(), userId);
            } catch (Exception e) {
                log.warn("failed to delete report file: fileId={}", report.getFileId(), e);
            }
        }
    }

    private ExamReport getOwnedReport(Long userId, Long id) {
        ExamReport report = reportMapper.selectById(id);
        if (report == null || !report.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.REPORT_NOT_FOUND);
        }
        return report;
    }

    private List<ExamIndicator> buildIndicators(ExamReport report, FamilyMember member,
                                                List<IndicatorItemRequest> items, String source) {
        Integer age = member.getBirthDate() == null
                ? null
                : Period.between(member.getBirthDate(), LocalDate.now()).getYears();
        List<ExamIndicator> result = new ArrayList<>();
        int fallbackSort = 0;
        for (IndicatorItemRequest item : items) {
            int sort = item.getSort() == null ? ++fallbackSort : item.getSort();
            ExamIndicator indicator = item.getMetricId() != null
                    ? indicatorAssembler.buildByMetricId(report, member, age, item.getMetricId(),
                            item.getValueText(), item.getUnit(), item.getRefRangeText(), source, sort)
                    : indicatorAssembler.buildByRawName(report, member, age, item.getMetricName().trim(),
                            item.getValueText(), item.getUnit(), item.getRefRangeText(), null, source, sort);
            result.add(indicator);
        }
        return result;
    }

    private MetricReferenceRange parseRangeText(String refRangeText) {
        String text = refRangeText.trim();
        Matcher between = RANGE_PATTERN.matcher(text);
        if (between.matches()) {
            MetricReferenceRange range = new MetricReferenceRange();
            range.setRefMin(new BigDecimal(between.group(1)));
            range.setRefMax(new BigDecimal(between.group(2)));
            range.setRefText(text);
            return range;
        }
        Matcher max = RANGE_MAX_PATTERN.matcher(text);
        if (max.matches()) {
            MetricReferenceRange range = new MetricReferenceRange();
            range.setRefMax(new BigDecimal(max.group(1)));
            range.setRefText(text);
            return range;
        }
        Matcher min = RANGE_MIN_PATTERN.matcher(text);
        if (min.matches()) {
            MetricReferenceRange range = new MetricReferenceRange();
            range.setRefMin(new BigDecimal(min.group(1)));
            range.setRefText(text);
            return range;
        }
        if ("阴性".equals(text)) {
            MetricReferenceRange range = new MetricReferenceRange();
            range.setNormalText("阴性");
            range.setRefText("阴性");
            return range;
        }
        return null;
    }

    private int countAbnormal(List<ExamIndicator> indicators) {
        return (int) indicators.stream()
                .filter(indicator -> ReportConstants.ABNORMAL_JUDGE_STATUSES.contains(indicator.getJudgeStatus()))
                .count();
    }

    private ReportDetailVO detail(ExamReport report, List<ExamIndicator> indicators) {
        List<IndicatorVO> indicatorVOs = indicators.stream().map(IndicatorVO::from).toList();
        return ReportDetailVO.builder()
                .report(ReportVO.from(report))
                .indicators(indicatorVOs)
                .build();
    }

    private void validateReportType(String reportType) {
        if (!ReportConstants.REPORT_TYPES.contains(reportType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "报告类型取值非法: " + reportType);
        }
    }

    private String defaultReportName(String reportName, FamilyMember member) {
        return StringUtils.hasText(reportName) ? reportName.trim() : member.getName() + "的体检报告";
    }
}
