package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.dto.admin.PromptUpsertRequest;
import com.health.assistant.entity.AiAsyncTask;
import com.health.assistant.entity.AiCallLog;
import com.health.assistant.entity.SysPromptTemplate;
import com.health.assistant.entity.SysUser;
import com.health.assistant.entity.SysUserToken;
import com.health.assistant.mapper.AiAsyncTaskMapper;
import com.health.assistant.mapper.AiCallLogMapper;
import com.health.assistant.mapper.SysPromptTemplateMapper;
import com.health.assistant.mapper.SysUserMapper;
import com.health.assistant.mapper.SysUserTokenMapper;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.AdminService;
import com.health.assistant.vo.AdminAiLogVO;
import com.health.assistant.vo.TaskVO;
import com.health.assistant.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SysUserMapper userMapper;
    private final SysUserTokenMapper tokenMapper;
    private final SysPromptTemplateMapper promptTemplateMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final AiAsyncTaskMapper taskMapper;
    private final AsyncTaskService asyncTaskService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> listUsers(String keyword, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .eq(status != null, SysUser::getStatus, status)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getNickname, keyword))
                .orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> page = userMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page, UserVO::from);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "状态取值非法");
        }
        if (userId.equals(SecurityUtils.getCurrentUserId()) && status == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能禁用当前登录账号");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.updateById(user);
        if (status == 0) {
            tokenMapper.update(null, Wrappers.<SysUserToken>lambdaUpdate()
                    .eq(SysUserToken::getUserId, userId)
                    .set(SysUserToken::getRevoked, 1));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        String tempPassword = generatePassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        userMapper.updateById(user);
        tokenMapper.update(null, Wrappers.<SysUserToken>lambdaUpdate()
                .eq(SysUserToken::getUserId, userId)
                .set(SysUserToken::getRevoked, 1));
        return tempPassword;
    }

    @Override
    public List<SysPromptTemplate> listPrompts(String code) {
        return promptTemplateMapper.selectList(Wrappers.<SysPromptTemplate>lambdaQuery()
                .eq(StringUtils.hasText(code), SysPromptTemplate::getCode, code)
                .orderByAsc(SysPromptTemplate::getCode)
                .orderByDesc(SysPromptTemplate::getVersion));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPromptTemplate createPromptVersion(PromptUpsertRequest request) {
        SysPromptTemplate latest = promptTemplateMapper.selectOne(Wrappers.<SysPromptTemplate>lambdaQuery()
                .eq(SysPromptTemplate::getCode, request.getCode())
                .orderByDesc(SysPromptTemplate::getVersion)
                .last("LIMIT 1"));
        int nextVersion = latest == null ? 1 : latest.getVersion() + 1;

        promptTemplateMapper.update(null, Wrappers.<SysPromptTemplate>lambdaUpdate()
                .eq(SysPromptTemplate::getCode, request.getCode())
                .set(SysPromptTemplate::getStatus, "DISABLED"));

        SysPromptTemplate template = new SysPromptTemplate();
        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setScene(StringUtils.hasText(request.getScene()) ? request.getScene() : request.getCode());
        template.setVersion(nextVersion);
        template.setContent(request.getContent());
        template.setTemperature(request.getTemperature());
        template.setMaxTokens(request.getMaxTokens());
        template.setStatus("ENABLED");
        template.setIsDefault(0);
        promptTemplateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePromptStatus(Long id, String status) {
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "状态取值非法");
        }
        SysPromptTemplate template = promptTemplateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        if ("ENABLED".equals(status)) {
            promptTemplateMapper.update(null, Wrappers.<SysPromptTemplate>lambdaUpdate()
                    .eq(SysPromptTemplate::getCode, template.getCode())
                    .set(SysPromptTemplate::getStatus, "DISABLED"));
        }
        template.setStatus(status);
        promptTemplateMapper.updateById(template);
    }

    @Override
    public PageResult<AdminAiLogVO> listAiLogs(String scene, Long userId, LocalDate dateFrom, LocalDate dateTo,
                                               long pageNum, long pageSize) {
        LambdaQueryWrapper<AiCallLog> wrapper = Wrappers.<AiCallLog>lambdaQuery()
                .eq(StringUtils.hasText(scene), AiCallLog::getScene, scene)
                .eq(userId != null, AiCallLog::getUserId, userId)
                .ge(dateFrom != null, AiCallLog::getCreatedAt, dateFrom == null ? null : dateFrom.atStartOfDay())
                .le(dateTo != null, AiCallLog::getCreatedAt, dateTo == null ? null : dateTo.plusDays(1).atStartOfDay())
                .orderByDesc(AiCallLog::getId);
        Page<AiCallLog> page = aiCallLogMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page, AdminAiLogVO::from);
    }

    @Override
    public List<Map<String, Object>> aiLogSummary(int days) {
        int range = Math.min(Math.max(days, 1), 90);
        LocalDate start = LocalDate.now().minusDays(range - 1L);
        List<AiCallLog> logs = aiCallLogMapper.selectList(Wrappers.<AiCallLog>lambdaQuery()
                .ge(AiCallLog::getCreatedAt, start.atStartOfDay())
                .orderByAsc(AiCallLog::getCreatedAt));
        Map<String, Map<String, Object>> grouped = new TreeMap<>();
        for (AiCallLog logEntry : logs) {
            if (logEntry.getCreatedAt() == null) {
                continue;
            }
            String day = logEntry.getCreatedAt().toLocalDate().format(DAY_FORMAT);
            Map<String, Object> item = grouped.computeIfAbsent(day, key -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("date", key);
                map.put("count", 0L);
                map.put("tokens", 0L);
                map.put("cost", BigDecimal.ZERO);
                return map;
            });
            item.put("count", (Long) item.get("count") + 1);
            item.put("tokens", (Long) item.get("tokens")
                    + (logEntry.getTotalTokens() == null ? 0 : logEntry.getTotalTokens()));
            item.put("cost", ((BigDecimal) item.get("cost"))
                    .add(logEntry.getCostEstimate() == null ? BigDecimal.ZERO : logEntry.getCostEstimate()));
        }
        return new ArrayList<>(grouped.values());
    }

    @Override
    public PageResult<TaskVO> listTasks(String status, String taskType, long pageNum, long pageSize) {
        Page<AiAsyncTask> page = taskMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<AiAsyncTask>lambdaQuery()
                        .eq(StringUtils.hasText(status), AiAsyncTask::getStatus, status)
                        .eq(StringUtils.hasText(taskType), AiAsyncTask::getTaskType, taskType)
                        .orderByDesc(AiAsyncTask::getId));
        return PageResult.of(page, TaskVO::from);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(Long taskId) {
        AiAsyncTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        if ("RUNNING".equals(task.getStatus()) || "PENDING".equals(task.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "任务正在进行中");
        }
        AiAsyncTask update = new AiAsyncTask();
        update.setId(taskId);
        update.setStatus("PENDING");
        update.setProgress(0);
        update.setRetryCount(0);
        update.setErrorMsg(null);
        update.setStartedAt(null);
        update.setFinishedAt(null);
        taskMapper.updateById(update);
        asyncTaskService.dispatch(taskId);
    }

    private String generatePassword() {
        while (true) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                builder.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            String password = builder.toString();
            boolean hasLetter = password.chars().anyMatch(Character::isLetter);
            boolean hasDigit = password.chars().anyMatch(Character::isDigit);
            if (hasLetter && hasDigit) {
                return password;
            }
        }
    }
}
