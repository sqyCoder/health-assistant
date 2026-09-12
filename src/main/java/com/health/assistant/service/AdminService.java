package com.health.assistant.service;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.entity.SysPromptTemplate;
import com.health.assistant.dto.admin.PromptUpsertRequest;
import com.health.assistant.vo.AdminAiLogVO;
import com.health.assistant.vo.TaskVO;
import com.health.assistant.vo.UserVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminService {

    PageResult<UserVO> listUsers(String keyword, Integer status, long pageNum, long pageSize);

    void updateUserStatus(Long userId, Integer status);

    String resetPassword(Long userId);

    List<SysPromptTemplate> listPrompts(String code);

    SysPromptTemplate createPromptVersion(PromptUpsertRequest request);

    void updatePromptStatus(Long id, String status);

    PageResult<AdminAiLogVO> listAiLogs(String scene, Long userId, LocalDate dateFrom, LocalDate dateTo,
                                        long pageNum, long pageSize);

    List<Map<String, Object>> aiLogSummary(int days);

    PageResult<TaskVO> listTasks(String status, String taskType, long pageNum, long pageSize);

    void retryTask(Long taskId);
}
