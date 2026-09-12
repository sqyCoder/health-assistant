package com.health.assistant.service;

import com.health.assistant.vo.ExportVO;

import java.util.Map;

public interface ExportService {

    Map<String, Long> create(String exportType, Long refId);

    ExportVO get(Long id);
}
