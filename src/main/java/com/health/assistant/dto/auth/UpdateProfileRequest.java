package com.health.assistant.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最长 50 个字符")
    private String nickname;

    @Size(max = 500, message = "头像地址最长 500 个字符")
    private String avatarUrl;
}
