package com.health.assistant.vo;

import com.health.assistant.entity.SysUser;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserVO {

    private final Long id;
    private final String username;
    private final String nickname;
    private final String phone;
    private final String email;
    private final String avatarUrl;
    private final String role;
    private final Integer status;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;

    public static UserVO from(SysUser user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
