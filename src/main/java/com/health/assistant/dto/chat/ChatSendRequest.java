package com.health.assistant.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatSendRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息最长 2000 个字符")
    private String content;
}
