package com.health.assistant.controller;

import com.health.assistant.common.api.Result;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.impl.AsyncTaskService;
import com.health.assistant.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "异步任务")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final AsyncTaskService asyncTaskService;

    @Operation(summary = "查询任务状态")
    @GetMapping("/{id}")
    public Result<TaskVO> getTask(@PathVariable Long id) {
        return Result.success(asyncTaskService.getTask(id, SecurityUtils.getCurrentUserId()));
    }
}
