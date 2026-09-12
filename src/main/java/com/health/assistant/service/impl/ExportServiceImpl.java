package com.health.assistant.service.impl;

import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.AiInterpretation;
import com.health.assistant.entity.AiSuggestion;
import com.health.assistant.entity.ExportRecord;
import com.health.assistant.mapper.AiInterpretationMapper;
import com.health.assistant.mapper.AiSuggestionMapper;
import com.health.assistant.mapper.ExportRecordMapper;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.ExportService;
import com.health.assistant.task.ExportPdfTaskHandler;
import com.health.assistant.vo.ExportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private static final String TYPE_INTERPRETATION = "INTERPRETATION";
    private static final String TYPE_SUGGESTION = "SUGGESTION";

    private final ExportRecordMapper exportRecordMapper;
    private final AiInterpretationMapper interpretationMapper;
    private final AiSuggestionMapper suggestionMapper;
    private final AsyncTaskService asyncTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Long> create(String exportType, Long refId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long memberId = resolveMemberId(userId, exportType, refId);
        ExportRecord record = new ExportRecord();
        record.setUserId(userId);
        record.setMemberId(memberId);
        record.setExportType(exportType);
        record.setRefId(refId);
        record.setStatus("PROCESSING");
        exportRecordMapper.insert(record);

        Long taskId = asyncTaskService.submit(ExportPdfTaskHandler.TASK_TYPE, userId,
                memberId, "EXPORT", record.getId(), null);

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("exportId", record.getId());
        result.put("taskId", taskId);
        return result;
    }

    @Override
    public ExportVO get(Long id) {
        ExportRecord record = exportRecordMapper.selectById(id);
        Long userId = SecurityUtils.getCurrentUserId();
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "导出记录不存在");
        }
        return ExportVO.from(record);
    }

    private Long resolveMemberId(Long userId, String exportType, Long refId) {
        if (TYPE_INTERPRETATION.equals(exportType)) {
            AiInterpretation interpretation = interpretationMapper.selectById(refId);
            if (interpretation == null || !interpretation.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.NOT_FOUND, "解读记录不存在");
            }
            if (!"DONE".equals(interpretation.getStatus())) {
                throw new BizException(ErrorCode.EXPORT_FAILED, "解读尚未完成，无法导出");
            }
            return interpretation.getMemberId();
        }
        if (TYPE_SUGGESTION.equals(exportType)) {
            AiSuggestion suggestion = suggestionMapper.selectById(refId);
            if (suggestion == null || !suggestion.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.NOT_FOUND, "建议记录不存在");
            }
            if (!"DONE".equals(suggestion.getStatus())) {
                throw new BizException(ErrorCode.EXPORT_FAILED, "建议尚未生成完成，无法导出");
            }
            return suggestion.getMemberId();
        }
        throw new BizException(ErrorCode.PARAM_ERROR, "不支持的导出类型: " + exportType);
    }
}
