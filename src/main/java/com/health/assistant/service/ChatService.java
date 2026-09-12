package com.health.assistant.service;

import com.health.assistant.dto.chat.ChatSessionCreateRequest;
import com.health.assistant.vo.ChatMessageVO;
import com.health.assistant.vo.ChatSessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    List<ChatSessionVO> listSessions(Long memberId);

    ChatSessionVO createSession(ChatSessionCreateRequest request);

    ChatSessionVO renameSession(Long id, String title);

    void deleteSession(Long id);

    List<ChatMessageVO> listMessages(Long sessionId);

    SseEmitter streamMessage(Long sessionId, String content);
}
