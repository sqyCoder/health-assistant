package com.health.assistant.controller;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.api.Result;
import com.health.assistant.dto.exercise.DailyRecordRequest;
import com.health.assistant.dto.exercise.WorkoutRequest;
import com.health.assistant.service.ExerciseService;
import com.health.assistant.vo.ExerciseDailyVO;
import com.health.assistant.vo.WorkoutSummaryVO;
import com.health.assistant.vo.WorkoutVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "运动记录")
@RestController
@RequestMapping("/api/members/{memberId}/exercise")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @Operation(summary = "查询某天日常记录")
    @GetMapping("/daily")
    public Result<ExerciseDailyVO> getDaily(@PathVariable Long memberId,
                                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(exerciseService.getDaily(memberId, date));
    }

    @Operation(summary = "保存日常记录（同日 upsert）")
    @PutMapping("/daily")
    public Result<ExerciseDailyVO> saveDaily(@PathVariable Long memberId,
                                             @Valid @RequestBody DailyRecordRequest request) {
        return Result.success(exerciseService.saveDaily(memberId, request));
    }

    @Operation(summary = "日常记录范围查询")
    @GetMapping("/daily/range")
    public Result<List<ExerciseDailyVO>> listDaily(
            @PathVariable Long memberId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        return Result.success(exerciseService.listDaily(memberId, from, to));
    }

    @Operation(summary = "删除日常记录")
    @DeleteMapping("/daily/{id}")
    public Result<Void> deleteDaily(@PathVariable Long memberId, @PathVariable Long id) {
        exerciseService.deleteDaily(memberId, id);
        return Result.success();
    }

    @Operation(summary = "训练记录分页")
    @GetMapping("/workouts")
    public Result<PageResult<WorkoutVO>> listWorkouts(
            @PathVariable Long memberId,
            @RequestParam(required = false) String workoutType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(exerciseService.listWorkouts(memberId, workoutType, from, to, pageNum, pageSize));
    }

    @Operation(summary = "新增训练记录")
    @PostMapping("/workouts")
    public Result<WorkoutVO> addWorkout(@PathVariable Long memberId,
                                        @Valid @RequestBody WorkoutRequest request) {
        return Result.success(exerciseService.addWorkout(memberId, request));
    }

    @Operation(summary = "修改训练记录")
    @PutMapping("/workouts/{id}")
    public Result<WorkoutVO> updateWorkout(@PathVariable Long memberId, @PathVariable Long id,
                                           @Valid @RequestBody WorkoutRequest request) {
        return Result.success(exerciseService.updateWorkout(memberId, id, request));
    }

    @Operation(summary = "删除训练记录")
    @DeleteMapping("/workouts/{id}")
    public Result<Void> deleteWorkout(@PathVariable Long memberId, @PathVariable Long id) {
        exerciseService.deleteWorkout(memberId, id);
        return Result.success();
    }

    @Operation(summary = "训练统计汇总")
    @GetMapping("/workouts/summary")
    public Result<WorkoutSummaryVO> summary(
            @PathVariable Long memberId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        return Result.success(exerciseService.summary(memberId, from, to));
    }
}
