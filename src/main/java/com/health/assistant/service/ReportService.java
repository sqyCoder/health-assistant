package com.health.assistant.service;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.dto.report.IndicatorReviewRequest;
import com.health.assistant.dto.report.ReportManualRequest;
import com.health.assistant.dto.report.ReportMetaUpdateRequest;
import com.health.assistant.vo.ReportDetailVO;
import com.health.assistant.vo.ReportVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface ReportService {

    ReportVO upload(MultipartFile file, Long memberId, String reportName, LocalDate reportDate,
                    String institution, String reportType);

    ReportDetailVO createManual(ReportManualRequest request);

    PageResult<ReportVO> list(Long memberId, String reportType, LocalDate dateFrom, LocalDate dateTo,
                              long pageNum, long pageSize);

    ReportDetailVO getDetail(Long id);

    ReportVO updateMeta(Long id, ReportMetaUpdateRequest request);

    ReportDetailVO updateIndicators(Long id, IndicatorReviewRequest request);

    Long parse(Long reportId);

    void delete(Long id);
}
