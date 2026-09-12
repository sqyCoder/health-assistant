package com.health.assistant.controller;

import com.health.assistant.common.api.Result;
import com.health.assistant.dto.chat.ChatSendRequest;
import com.health.assistant.dto.chat.ChatSessionCreateRequest;
import com.health.assistant.service.ChatService;
import com.health.assistant.vo.ChatMessageVO;
import com.health.assistant.vo.ChatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Tag(name = "健康对话")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "会话列表")
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> listSessions(@RequestParam Long memberId) {
        return Result.success(chatService.listSessions(memberId));
    }

    @Operation(summary = "新建会话")
    @PostMapping("/sessions")
    public Result<ChatSessionVO> createSession(@Valid @RequestBody ChatSessionCreateRequest request) {
        return Result.success(chatService.createSession(request));
    }

    @Operation(summary = "重命名会话")
    @PutMapping("/sessions/{id}")
    public Result<ChatSessionVO> renameSession(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(chatService.renameSession(id, body.get("title")));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return Result.success();
    }

    @Operation(summary = "历史消息")
    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessageVO>> listMessages(@PathVariable Long id) {
        return Result.success(chatService.listMessages(id));
    }

    @Operation(summary = "发送消息（SSE 流式）")
    @PostMapping(value = "/sessions/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id, @Valid @RequestBody ChatSendRequest request) {
        return chatService.streamMessage(id, request.getContent());
    }
}
