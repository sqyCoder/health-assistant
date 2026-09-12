package com.health.assistant.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ChartService {

    Map<String, Object> overview(Long memberId);

    List<Map<String, Object>> indicatorTrend(Long memberId, String metricCode, LocalDate from, LocalDate to);

    List<Map<String, Object>> exerciseTrend(Long memberId, String granularity, LocalDate from, LocalDate to);

    List<Map<String, Object>> workoutDistribution(Long memberId, LocalDate from, LocalDate to);

    List<Map<String, Object>> vitalTrend(Long memberId, String type, LocalDate from, LocalDate to);
}
