package com.health.assistant.task;

import com.health.assistant.entity.AiAsyncTask;

public interface TaskHandler {

    String getTaskType();

    String handle(AiAsyncTask task);
}
