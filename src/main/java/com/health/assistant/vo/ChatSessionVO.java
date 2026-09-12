package com.health.assistant.vo;

import com.health.assistant.entity.ChatSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatSessionVO {

    private final Long id;
    private final Long memberId;
    private final String title;
    private final LocalDateTime lastMessageAt;
    private final Integer messageCount;
    private final LocalDateTime createdAt;

    public static ChatSessionVO from(ChatSession entity) {
        return ChatSessionVO.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .title(entity.getTitle())
                .lastMessageAt(entity.getLastMessageAt())
                .messageCount(entity.getMessageCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
