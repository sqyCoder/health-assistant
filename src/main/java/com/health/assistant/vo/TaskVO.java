package com.health.assistant.vo;

import com.health.assistant.entity.AiAsyncTask;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskVO {

    private final Long id;
    private final String taskType;
    private final String bizType;
    private final Long bizId;
    private final String status;
    private final Integer progress;
    private final String errorMsg;
    private final String resultJson;
    private final LocalDateTime createdAt;
    private final LocalDateTime finishedAt;

    public static TaskVO from(AiAsyncTask entity) {
        return TaskVO.builder()
                .id(entity.getId())
                .taskType(entity.getTaskType())
                .bizType(entity.getBizType())
                .bizId(entity.getBizId())
                .status(entity.getStatus())
                .progress(entity.getProgress())
                .errorMsg(entity.getErrorMsg())
                .resultJson(entity.getResultJson())
                .createdAt(entity.getCreatedAt())
                .finishedAt(entity.getFinishedAt())
                .build();
    }
}
