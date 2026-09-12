package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.AiInterpretation;
import com.health.assistant.entity.ExamReport;
import com.health.assistant.mapper.AiInterpretationMapper;
import com.health.assistant.mapper.ExamReportMapper;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.InterpretationService;
import com.health.assistant.support.AiQuotaService;
import com.health.assistant.support.MemberAccessChecker;
import com.health.assistant.task.AiInterpretTaskHandler;
import com.health.assistant.vo.InterpretationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterpretationServiceImpl implements InterpretationService {

    private final ExamReportMapper reportMapper;
    private final AiInterpretationMapper interpretationMapper;
    private final MemberAccessChecker memberAccessChecker;
    private final AsyncTaskService asyncTaskService;
    private final AiQuotaService aiQuotaService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> generate(Long reportId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ExamReport report = getOwnedReport(userId, reportId);
        memberAccessChecker.check(userId, report.getMemberId());
        aiQuotaService.checkQuota(userId);
        if (report.getIndicatorCount() == null || report.getIndicatorCount() == 0) {
            throw new BizException(ErrorCode.AI_GENERATE_FAILED, "报告还没有指标，请先完成解析或手动录入");
        }
        AiInterpretation interpretation = new AiInterpretation();
        interpretation.setReportId(reportId);
        interpretation.setMemberId(report.getMemberId());
        interpretation.setUserId(userId);
        interpretation.setStatus("PROCESSING");
        interpretation.setAbnormalCount(report.getAbnormalCount() == null ? 0 : report.getAbnormalCount());
        interpretationMapper.insert(interpretation);

        report.setAiStatus("PROCESSING");
        reportMapper.updateById(report);

        Long taskId = asyncTaskService.submit(AiInterpretTaskHandler.TASK_TYPE, userId,
                report.getMemberId(), "INTERPRETATION", interpretation.getId(), null);

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("interpretationId", interpretation.getId());
        result.put("taskId", taskId);
        return result;
    }

    @Override
    public InterpretationVO getLatest(Long reportId) {
        Long userId = SecurityUtils.getCurrentUserId();
        getOwnedReport(userId, reportId);
        AiInterpretation latest = interpretationMapper.selectOne(Wrappers.<AiInterpretation>lambdaQuery()
                .eq(AiInterpretation::getReportId, reportId)
                .orderByDesc(AiInterpretation::getId)
                .last("LIMIT 1"));
        return latest == null ? null : InterpretationVO.from(latest);
    }

    private ExamReport getOwnedReport(Long userId, Long reportId) {
        ExamReport report = reportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.REPORT_NOT_FOUND);
        }
        return report;
    }
}
