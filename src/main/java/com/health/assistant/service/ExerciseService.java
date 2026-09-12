package com.health.assistant.service;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.dto.exercise.DailyRecordRequest;
import com.health.assistant.dto.exercise.WorkoutRequest;
import com.health.assistant.vo.ExerciseDailyVO;
import com.health.assistant.vo.WorkoutSummaryVO;
import com.health.assistant.vo.WorkoutVO;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseService {

    ExerciseDailyVO getDaily(Long memberId, LocalDate date);

    ExerciseDailyVO saveDaily(Long memberId, DailyRecordRequest request);

    List<ExerciseDailyVO> listDaily(Long memberId, LocalDate from, LocalDate to);

    void deleteDaily(Long memberId, Long id);

    PageResult<WorkoutVO> listWorkouts(Long memberId, String workoutType, LocalDate from, LocalDate to,
                                       long pageNum, long pageSize);

    WorkoutVO addWorkout(Long memberId, WorkoutRequest request);

    WorkoutVO updateWorkout(Long memberId, Long id, WorkoutRequest request);

    void deleteWorkout(Long memberId, Long id);

    WorkoutSummaryVO summary(Long memberId, LocalDate from, LocalDate to);
}
