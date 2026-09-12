package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.entity.AiAsyncTask;
import com.health.assistant.mapper.AiAsyncTaskMapper;
import com.health.assistant.task.TaskHandler;
import com.health.assistant.vo.TaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AsyncTaskService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private final AiAsyncTaskMapper taskMapper;
    private final ThreadPoolTaskExecutor aiTaskExecutor;
    private final ScheduledExecutorService aiTaskScheduler;
    private final Map<String, TaskHandler> handlers;

    public AsyncTaskService(AiAsyncTaskMapper taskMapper,
                            @Qualifier("aiTaskExecutor") ThreadPoolTaskExecutor aiTaskExecutor,
                            @Qualifier("aiTaskScheduler") ScheduledExecutorService aiTaskScheduler,
                            List<TaskHandler> handlerList) {
        this.taskMapper = taskMapper;
        this.aiTaskExecutor = aiTaskExecutor;
        this.aiTaskScheduler = aiTaskScheduler;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(TaskHandler::getTaskType, Function.identity(), (a, b) -> a));
    }

    public Long submit(String taskType, Long userId, Long memberId, String bizType, Long bizId, String payloadJson) {
        AiAsyncTask task = new AiAsyncTask();
        task.setUserId(userId);
        task.setMemberId(memberId);
        task.setTaskType(taskType);
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setStatus(STATUS_PENDING);
        task.setProgress(0);
        task.setPayloadJson(payloadJson);
        task.setRetryCount(0);
        task.setMaxRetry(2);
        taskMapper.insert(task);
        dispatch(task.getId());
        return task.getId();
    }

    public void dispatch(Long taskId) {
        aiTaskExecutor.execute(() -> run(taskId));
    }

    public TaskVO getTask(Long taskId, Long userId) {
        AiAsyncTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        return TaskVO.from(task);
    }

    private void run(Long taskId) {
        AiAsyncTask snapshot = taskMapper.selectById(taskId);
        if (snapshot == null || !STATUS_PENDING.equals(snapshot.getStatus())) {
            return;
        }
        int claimed = taskMapper.update(null, Wrappers.<AiAsyncTask>lambdaUpdate()
                .eq(AiAsyncTask::getId, taskId)
                .eq(AiAsyncTask::getStatus, STATUS_PENDING)
                .set(AiAsyncTask::getStatus, STATUS_RUNNING)
                .set(AiAsyncTask::getStartedAt, LocalDateTime.now())
                .set(AiAsyncTask::getProgress, 10)
                .set(AiAsyncTask::getErrorMsg, null));
        if (claimed == 0) {
            return;
        }
        AiAsyncTask task = taskMapper.selectById(taskId);
        TaskHandler handler = handlers.get(task.getTaskType());
        try {
            if (handler == null) {
                throw new BizException(ErrorCode.SYSTEM_ERROR, "未知任务类型: " + task.getTaskType());
            }
            String resultJson = handler.handle(task);
            AiAsyncTask done = new AiAsyncTask();
            done.setId(taskId);
            done.setStatus(STATUS_SUCCESS);
            done.setProgress(100);
            done.setResultJson(resultJson);
            done.setFinishedAt(LocalDateTime.now());
            taskMapper.updateById(done);
        } catch (Exception e) {
            log.error("task failed: id={}, type={}", taskId, task.getTaskType(), e);
            int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
            int maxRetry = task.getMaxRetry() == null ? 2 : task.getMaxRetry();
            if (retryCount < maxRetry) {
                AiAsyncTask retry = new AiAsyncTask();
                retry.setId(taskId);
                retry.setStatus(STATUS_PENDING);
                retry.setProgress(0);
                retry.setRetryCount(retryCount + 1);
                retry.setErrorMsg(truncate(e.getMessage(), 1000));
                taskMapper.updateById(retry);
                aiTaskScheduler.schedule(() -> dispatch(taskId), 5, TimeUnit.SECONDS);
            } else {
                AiAsyncTask failed = new AiAsyncTask();
                failed.setId(taskId);
                failed.setStatus(STATUS_FAILED);
                failed.setErrorMsg(truncate(e.getMessage(), 1000));
                failed.setFinishedAt(LocalDateTime.now());
                taskMapper.updateById(failed);
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverTasks() {
        try {
            int zombie = taskMapper.update(null, Wrappers.<AiAsyncTask>lambdaUpdate()
                    .eq(AiAsyncTask::getStatus, STATUS_RUNNING)
                    .set(AiAsyncTask::getStatus, STATUS_FAILED)
                    .set(AiAsyncTask::getErrorMsg, "应用重启导致任务中断"));
            if (zombie > 0) {
                log.warn("marked {} zombie tasks as failed", zombie);
            }
            List<AiAsyncTask> pending = taskMapper.selectList(Wrappers.<AiAsyncTask>lambdaQuery()
                    .eq(AiAsyncTask::getStatus, STATUS_PENDING));
            if (!pending.isEmpty()) {
                log.info("re-dispatching {} pending tasks", pending.size());
                pending.forEach(task -> dispatch(task.getId()));
            }
        } catch (Exception e) {
            log.error("failed to recover tasks", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "unknown error";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
