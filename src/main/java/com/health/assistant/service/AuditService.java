package com.health.assistant.service;

import com.health.assistant.common.web.ClientInfo;

public interface AuditService {

    void record(Long userId, String username, String action, String resourceType, Long resourceId, ClientInfo clientInfo);
}
