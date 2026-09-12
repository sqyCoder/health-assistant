package com.health.assistant.service.impl;

import com.health.assistant.common.web.ClientInfo;
import com.health.assistant.entity.SysAuditLog;
import com.health.assistant.mapper.SysAuditLogMapper;
import com.health.assistant.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final int MAX_USER_AGENT_LENGTH = 300;

    private final SysAuditLogMapper auditLogMapper;

    @Override
    public void record(Long userId, String username, String action, String resourceType, Long resourceId, ClientInfo clientInfo) {
        try {
            SysAuditLog auditLog = new SysAuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            if (clientInfo != null) {
                auditLog.setIp(clientInfo.ip());
                String userAgent = clientInfo.userAgent();
                if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
                    userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
                }
                auditLog.setUserAgent(userAgent);
            }
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("failed to write audit log: action={}, userId={}", action, userId, e);
        }
    }
}
