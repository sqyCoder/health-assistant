package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.constant.ExerciseConstants;
import com.health.assistant.common.constant.MemberConstants;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.dto.exercise.DailyRecordRequest;
import com.health.assistant.dto.exercise.WorkoutRequest;
import com.health.assistant.entity.ExerciseDaily;
import com.health.assistant.entity.ExerciseWorkout;
import com.health.assistant.entity.FamilyMember;
import com.health.assistant.mapper.ExerciseDailyMapper;
import com.health.assistant.mapper.ExerciseWorkoutMapper;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.ExerciseService;
import com.health.assistant.support.MemberAccessChecker;
import com.health.assistant.vo.ExerciseDailyVO;
import com.health.assistant.vo.WorkoutSummaryVO;
import com.health.assistant.vo.WorkoutVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseDailyMapper dailyMapper;
    private final ExerciseWorkoutMapper workoutMapper;
    private final MemberAccessChecker memberAccessChecker;

    @Override
    public ExerciseDailyVO getDaily(Long memberId, LocalDate date) {
        FamilyMember member = memberAccessChecker.check(SecurityUtils.getCurrentUserId(), memberId);
        return ExerciseDailyVO.from(findDaily(member.getId(), date));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExerciseDailyVO saveDaily(Long memberId, DailyRecordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        validateOptional(MemberConstants.SLEEP_QUALITIES, request.getSleepQuality(), "睡眠质量");
        validateOptional(ExerciseConstants.GLUCOSE_TYPES, request.getGlucoseType(), "血糖类型");
        validateOptional(ExerciseConstants.MOODS, request.getMood(), "心情");

        ExerciseDaily entity = findDaily(member.getId(), request.getRecordDate());
        if (entity == null) {
            entity = new ExerciseDaily();
            entity.setMemberId(member.getId());
            entity.setUserId(userId);
            entity.setRecordDate(request.getRecordDate());
        }
        applyDaily(entity, request);
        if (entity.getId() == null) {
            dailyMapper.insert(entity);
        } else {
            dailyMapper.updateById(entity);
        }
        return ExerciseDailyVO.from(entity);
    }

    @Override
    public List<ExerciseDailyVO> listDaily(Long memberId, LocalDate from, LocalDate to) {
        FamilyMember member = memberAccessChecker.check(SecurityUtils.getCurrentUserId(), memberId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        return dailyMapper.selectList(Wrappers.<ExerciseDaily>lambdaQuery()
                        .eq(ExerciseDaily::getMemberId, member.getId())
                        .ge(ExerciseDaily::getRecordDate, start)
                        .le(ExerciseDaily::getRecordDate, end)
                        .orderByDesc(ExerciseDaily::getRecordDate))
                .stream()
                .map(ExerciseDailyVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDaily(Long memberId, Long id) {
        FamilyMember member = memberAccessChecker.check(SecurityUtils.getCurrentUserId(), memberId);
        ExerciseDaily entity = dailyMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "日常记录不存在");
        }
        dailyMapper.deleteById(id);
    }

    @Override
    public PageResult<WorkoutVO> listWorkouts(Long memberId, String workoutType, LocalDate from, LocalDate to,
                                              long pageNum, long pageSize) {
        FamilyMember member = memberAccessChecker.check(SecurityUtils.getCurrentUserId(), memberId);
        LambdaQueryWrapper<ExerciseWorkout> wrapper = Wrappers.<ExerciseWorkout>lambdaQuery()
                .eq(ExerciseWorkout::getMemberId, member.getId())
                .eq(StringUtils.hasText(workoutType), ExerciseWorkout::getWorkoutType, workoutType)
                .ge(from != null, ExerciseWorkout::getWorkoutDate, from)
                .le(to != null, ExerciseWorkout::getWorkoutDate, to)
                .orderByDesc(ExerciseWorkout::getWorkoutDate)
                .orderByDesc(ExerciseWorkout::getId);
        Page<ExerciseWorkout> page = workoutMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page, WorkoutVO::from);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkoutVO addWorkout(Long memberId, WorkoutRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        FamilyMember member = memberAccessChecker.check(userId, memberId);
        validateWorkout(request);
        ExerciseWorkout entity = new ExerciseWorkout();
        entity.setMemberId(member.getId());
        entity.setUserId(userId);
        applyWorkout(entity, request);
        workoutMapper.insert(entity);
        return WorkoutVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkoutVO updateWorkout(Long memberId, Long id, WorkoutRequest request) {
        FamilyMember member = memberAccessChecker.check(SecurityUtils.getCurrentUserId(), memberId);
        validateWorkout(request);
        ExerciseWorkout entity = workoutMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "训练记录不存在");
        }
        applyWorkout(entity, request);
        workoutMapper.updateById(entity);
        return WorkoutVO.from(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkout(Long memberId, Long id) {
        FamilyMember member = memberAccessChecker.check(SecurityUtils.getCurrentUserId(), memberId);
        ExerciseWorkout entity = workoutMapper.selectById(id);
        if (entity == null || !entity.getMemberId().equals(member.getId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "训练记录不存在");
        }
        workoutMapper.deleteById(id);
    }

    @Override
    public WorkoutSummaryVO summary(Long memberId, LocalDate from, LocalDate to) {
        FamilyMember member = memberAccessChecker.check(SecurityUtils.getCurrentUserId(), memberId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        List<ExerciseWorkout> workouts = workoutMapper.selectList(Wrappers.<ExerciseWorkout>lambdaQuery()
                .eq(ExerciseWorkout::getMemberId, member.getId())
                .ge(ExerciseWorkout::getWorkoutDate, start)
                .le(ExerciseWorkout::getWorkoutDate, end));

        long totalCount = workouts.size();
        long totalMinutes = workouts.stream()
                .mapToLong(workout -> workout.getDurationMin() == null ? 0 : workout.getDurationMin())
                .sum();
        BigDecimal totalCalories = workouts.stream()
                .map(workout -> workout.getCaloriesKcal() == null ? BigDecimal.ZERO : workout.getCaloriesKcal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDistance = workouts.stream()
                .map(workout -> workout.getDistanceKm() == null ? BigDecimal.ZERO : workout.getDistanceKm())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<ExerciseWorkout>> grouped = workouts.stream()
                .collect(Collectors.groupingBy(ExerciseWorkout::getWorkoutType));
        List<WorkoutSummaryVO.TypeStat> stats = new ArrayList<>();
        for (Map.Entry<String, List<ExerciseWorkout>> entry : grouped.entrySet()) {
            long minutes = entry.getValue().stream()
                    .mapToLong(workout -> workout.getDurationMin() == null ? 0 : workout.getDurationMin())
                    .sum();
            BigDecimal calories = entry.getValue().stream()
                    .map(workout -> workout.getCaloriesKcal() == null ? BigDecimal.ZERO : workout.getCaloriesKcal())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.add(WorkoutSummaryVO.TypeStat.builder()
                    .type(entry.getKey())
                    .count(entry.getValue().size())
                    .minutes(minutes)
                    .calories(calories)
                    .build());
        }
        stats.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        return WorkoutSummaryVO.builder()
                .totalCount(totalCount)
                .totalMinutes(totalMinutes)
                .totalCalories(totalCalories)
                .totalDistanceKm(totalDistance)
                .byType(stats)
                .build();
    }

    private ExerciseDaily findDaily(Long memberId, LocalDate date) {
        return dailyMapper.selectOne(Wrappers.<ExerciseDaily>lambdaQuery()
                .eq(ExerciseDaily::getMemberId, memberId)
                .eq(ExerciseDaily::getRecordDate, date));
    }

    private void applyDaily(ExerciseDaily entity, DailyRecordRequest request) {
        entity.setSteps(request.getSteps());
        entity.setActiveMinutes(request.getActiveMinutes());
        entity.setCaloriesKcal(request.getCaloriesKcal());
        entity.setDistanceKm(request.getDistanceKm());
        entity.setSleepHours(request.getSleepHours());
        entity.setSleepQuality(request.getSleepQuality());
        entity.setRestingHeartRate(request.getRestingHeartRate());
        entity.setWeightKg(request.getWeightKg());
        entity.setSystolicBp(request.getSystolicBp());
        entity.setDiastolicBp(request.getDiastolicBp());
        entity.setBloodGlucose(request.getBloodGlucose());
        entity.setGlucoseType(request.getGlucoseType());
        entity.setMood(request.getMood());
        entity.setNote(request.getNote());
    }

    private void applyWorkout(ExerciseWorkout entity, WorkoutRequest request) {
        entity.setWorkoutDate(request.getWorkoutDate());
        entity.setStartTime(request.getStartTime());
        entity.setWorkoutType(request.getWorkoutType());
        entity.setTitle(request.getTitle());
        entity.setDurationMin(request.getDurationMin());
        entity.setIntensity(request.getIntensity());
        entity.setDistanceKm(request.getDistanceKm());
        entity.setAvgHeartRate(request.getAvgHeartRate());
        entity.setMaxHeartRate(request.getMaxHeartRate());
        entity.setPaceSecPerKm(request.getPaceSecPerKm());
        entity.setCaloriesKcal(request.getCaloriesKcal());
        entity.setFeeling(request.getFeeling());
        entity.setNote(request.getNote());
    }

    private void validateWorkout(WorkoutRequest request) {
        requireIn(ExerciseConstants.WORKOUT_TYPES, request.getWorkoutType(), "运动类型");
        validateOptional(ExerciseConstants.INTENSITIES, request.getIntensity(), "运动强度");
        validateOptional(ExerciseConstants.FEELINGS, request.getFeeling(), "运动感受");
    }

    private void requireIn(Set<String> allowed, String value, String fieldName) {
        if (!allowed.contains(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, fieldName + "取值非法: " + value);
        }
    }

    private void validateOptional(Set<String> allowed, String value, String fieldName) {
        if (StringUtils.hasText(value) && !allowed.contains(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, fieldName + "取值非法: " + value);
        }
    }
}
