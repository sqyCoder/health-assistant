package com.health.assistant.vo;

import com.health.assistant.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageVO {

    private final Long id;
    private final String role;
    private final String content;
    private final String status;
    private final Integer tokenIn;
    private final Integer tokenOut;
    private final Integer durationMs;
    private final String errorMsg;
    private final LocalDateTime createdAt;

    public static ChatMessageVO from(ChatMessage entity) {
        return ChatMessageVO.builder()
                .id(entity.getId())
                .role(entity.getRole())
                .content(entity.getContent())
                .status(entity.getStatus())
                .tokenIn(entity.getTokenIn())
                .tokenOut(entity.getTokenOut())
                .durationMs(entity.getDurationMs())
                .errorMsg(entity.getErrorMsg())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
