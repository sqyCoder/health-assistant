package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.ExamIndicator;
import com.health.assistant.entity.ExamReport;
import com.health.assistant.entity.ExerciseDaily;
import com.health.assistant.entity.ExerciseWorkout;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.mapper.ExamIndicatorMapper;
import com.health.assistant.mapper.ExamReportMapper;
import com.health.assistant.mapper.ExerciseDailyMapper;
import com.health.assistant.mapper.ExerciseWorkoutMapper;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.ChartService;
import com.health.assistant.support.MemberAccessChecker;
import com.health.assistant.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class ChartServiceImpl implements ChartService {

    private final MemberAccessChecker memberAccessChecker;
    private final ExamReportMapper reportMapper;
    private final ExamIndicatorMapper indicatorMapper;
    private final ExerciseDailyMapper dailyMapper;
    private final ExerciseWorkoutMapper workoutMapper;

    @Override
    public Map<String, Object> overview(Long memberId) {
        FamilyMember member = access(memberId);
        MemberVO memberVO = MemberVO.from(member);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("memberName", member.getName());
        data.put("bmi", memberVO.getBmi());
        data.put("age", memberVO.getAge());
        data.put("gender", member.getGender());

        ExamReport latestReport = reportMapper.selectOne(Wrappers.<ExamReport>lambdaQuery()
                .eq(ExamReport::getMemberId, member.getId())
                .orderByDesc(ExamReport::getReportDate)
                .orderByDesc(ExamReport::getId)
                .last("LIMIT 1"));
        data.put("lastReportDate", latestReport == null ? null : latestReport.getReportDate());
        data.put("abnormalCount", latestReport == null || latestReport.getAbnormalCount() == null
                ? 0 : latestReport.getAbnormalCount());

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        List<ExerciseDaily> weekDaily = dailyMapper.selectList(Wrappers.<ExerciseDaily>lambdaQuery()
                .eq(ExerciseDaily::getMemberId, member.getId())
                .ge(ExerciseDaily::getRecordDate, weekStart)
                .le(ExerciseDaily::getRecordDate, today));
        int weekActiveMinutes = weekDaily.stream()
                .mapToInt(item -> item.getActiveMinutes() == null ? 0 : item.getActiveMinutes())
                .sum();
        int weekSteps = weekDaily.stream()
                .mapToInt(item -> item.getSteps() == null ? 0 : item.getSteps())
                .sum();
        data.put("weekActiveMinutes", weekActiveMinutes);
        data.put("weekSteps", weekSteps);
        long weekWorkoutCount = workoutMapper.selectCount(Wrappers.<ExerciseWorkout>lambdaQuery()
                .eq(ExerciseWorkout::getMemberId, member.getId())
                .ge(ExerciseWorkout::getWorkoutDate, weekStart)
                .le(ExerciseWorkout::getWorkoutDate, today));
        data.put("weekWorkoutCount", weekWorkoutCount);

        ExerciseDaily latestWeight = dailyMapper.selectOne(Wrappers.<ExerciseDaily>lambdaQuery()
                .eq(ExerciseDaily::getMemberId, member.getId())
                .isNotNull(ExerciseDaily::getWeightKg)
                .orderByDesc(ExerciseDaily::getRecordDate)
                .last("LIMIT 1"));
        data.put("latestWeightKg", latestWeight != null ? latestWeight.getWeightKg() : member.getWeightKg());
        return data;
    }

    @Override
    public List<Map<String, Object>> indicatorTrend(Long memberId, String metricCode,
                                                    LocalDate from, LocalDate to) {
        if (metricCode == null || metricCode.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "metricCode 不能为空");
        }
        FamilyMember member = access(memberId);
        List<ExamReport> reports = reportMapper.selectList(Wrappers.<ExamReport>lambdaQuery()
                .eq(ExamReport::getMemberId, member.getId())
                .ge(from != null, ExamReport::getReportDate, from)
                .le(to != null, ExamReport::getReportDate, to)
                .orderByAsc(ExamReport::getReportDate));
        if (reports.isEmpty()) {
            return List.of();
        }
        Map<Long, ExamReport> reportById = new LinkedHashMap<>();
        reports.forEach(report -> reportById.put(report.getId(), report));

        List<ExamIndicator> indicators = indicatorMapper.selectList(Wrappers.<ExamIndicator>lambdaQuery()
                .in(ExamIndicator::getReportId, reportById.keySet())
                .eq(ExamIndicator::getMetricCode, metricCode)
                .isNotNull(ExamIndicator::getValueNumeric));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamIndicator indicator : indicators) {
            ExamReport report = reportById.get(indicator.getReportId());
            if (report == null) {
                continue;
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", report.getReportDate());
            point.put("reportId", report.getId());
            point.put("value", indicator.getValueNumeric());
            point.put("unit", indicator.getUnit());
            point.put("status", indicator.getJudgeStatus());
            result.add(point);
        }
        result.sort(Comparator.comparing(point -> (LocalDate) point.get("date")));
        return result;
    }

    @Override
    public List<Map<String, Object>> exerciseTrend(Long memberId, String granularity,
                                                   LocalDate from, LocalDate to) {
        FamilyMember member = access(memberId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        List<ExerciseDaily> dailies = dailyMapper.selectList(Wrappers.<ExerciseDaily>lambdaQuery()
                .eq(ExerciseDaily::getMemberId, member.getId())
                .ge(ExerciseDaily::getRecordDate, start)
                .le(ExerciseDaily::getRecordDate, end)
                .orderByAsc(ExerciseDaily::getRecordDate));

        List<Map<String, Object>> result = new ArrayList<>();
        if ("week".equalsIgnoreCase(granularity)) {
            Map<LocalDate, BigDecimal[]> grouped = new TreeMap<>();
            for (ExerciseDaily daily : dailies) {
                LocalDate weekStart = daily.getRecordDate().with(DayOfWeek.MONDAY);
                BigDecimal[] aggregate = grouped.computeIfAbsent(weekStart,
                        key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                aggregate[0] = aggregate[0].add(BigDecimal.valueOf(daily.getSteps() == null ? 0 : daily.getSteps()));
                aggregate[1] = aggregate[1].add(BigDecimal.valueOf(
                        daily.getActiveMinutes() == null ? 0 : daily.getActiveMinutes()));
                aggregate[2] = aggregate[2].add(
                        daily.getCaloriesKcal() == null ? BigDecimal.ZERO : daily.getCaloriesKcal());
            }
            for (Map.Entry<LocalDate, BigDecimal[]> entry : grouped.entrySet()) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", entry.getKey());
                point.put("steps", entry.getValue()[0]);
                point.put("activeMinutes", entry.getValue()[1]);
                point.put("calories", entry.getValue()[2]);
                result.add(point);
            }
        } else {
            for (ExerciseDaily daily : dailies) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", daily.getRecordDate());
                point.put("steps", daily.getSteps());
                point.put("activeMinutes", daily.getActiveMinutes());
                point.put("calories", daily.getCaloriesKcal());
                result.add(point);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> workoutDistribution(Long memberId, LocalDate from, LocalDate to) {
        FamilyMember member = access(memberId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        List<ExerciseWorkout> workouts = workoutMapper.selectList(Wrappers.<ExerciseWorkout>lambdaQuery()
                .eq(ExerciseWorkout::getMemberId, member.getId())
                .ge(ExerciseWorkout::getWorkoutDate, start)
                .le(ExerciseWorkout::getWorkoutDate, end));

        Map<String, long[]> grouped = new TreeMap<>();
        Map<String, BigDecimal> caloriesByType = new TreeMap<>();
        for (ExerciseWorkout workout : workouts) {
            long[] aggregate = grouped.computeIfAbsent(workout.getWorkoutType(), key -> new long[2]);
            aggregate[0] += 1;
            aggregate[1] += workout.getDurationMin() == null ? 0 : workout.getDurationMin();
            caloriesByType.merge(workout.getWorkoutType(),
                    workout.getCaloriesKcal() == null ? BigDecimal.ZERO : workout.getCaloriesKcal(),
                    BigDecimal::add);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : grouped.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("type", entry.getKey());
            point.put("count", entry.getValue()[0]);
            point.put("minutes", entry.getValue()[1]);
            point.put("calories", caloriesByType.get(entry.getKey()));
            result.add(point);
        }
        result.sort(Comparator.comparingLong(point -> -((Long) point.get("count"))));
        return result;
    }

    @Override
    public List<Map<String, Object>> vitalTrend(Long memberId, String type, LocalDate from, LocalDate to) {
        FamilyMember member = access(memberId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(89);
        List<ExerciseDaily> dailies = dailyMapper.selectList(Wrappers.<ExerciseDaily>lambdaQuery()
                .eq(ExerciseDaily::getMemberId, member.getId())
                .ge(ExerciseDaily::getRecordDate, start)
                .le(ExerciseDaily::getRecordDate, end)
                .orderByAsc(ExerciseDaily::getRecordDate));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExerciseDaily daily : dailies) {
            Map<String, Object> point = new LinkedHashMap<>();
            switch (type == null ? "weight" : type) {
                case "bp" -> {
                    if (daily.getSystolicBp() == null && daily.getDiastolicBp() == null) {
                        continue;
                    }
                    point.put("date", daily.getRecordDate());
                    point.put("systolic", daily.getSystolicBp());
                    point.put("diastolic", daily.getDiastolicBp());
                }
                case "glucose" -> {
                    if (daily.getBloodGlucose() == null) {
                        continue;
                    }
                    point.put("date", daily.getRecordDate());
                    point.put("value", daily.getBloodGlucose());
                    point.put("glucoseType", daily.getGlucoseType());
                }
                default -> {
                    if (daily.getWeightKg() == null) {
                        continue;
                    }
                    point.put("date", daily.getRecordDate());
                    point.put("value", daily.getWeightKg());
                }
            }
            result.add(point);
        }
        return result;
    }

    private FamilyMember access(Long memberId) {
        return memberAccessChecker.check(SecurityUtils.getCurrentUserId(), memberId);
    }
}
