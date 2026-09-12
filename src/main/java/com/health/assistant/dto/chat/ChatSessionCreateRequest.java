package com.health.assistant.dto.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatSessionCreateRequest {

    @NotNull(message = "成员不能为空")
    private Long memberId;

    @Size(max = 100, message = "标题最长 100 个字符")
    private String title;
}
