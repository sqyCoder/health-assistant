package com.health.assistant.service;

import com.health.assistant.vo.InterpretationVO;

import java.util.Map;

public interface InterpretationService {

    Map<String, Long> generate(Long reportId);

    InterpretationVO getLatest(Long reportId);
}
