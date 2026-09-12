package com.health.assistant.service;

import com.health.assistant.common.web.ClientInfo;
import com.health.assistant.dto.auth.ChangePasswordRequest;
import com.health.assistant.dto.auth.LoginRequest;
import com.health.assistant.dto.auth.RegisterRequest;
import com.health.assistant.dto.auth.UpdateProfileRequest;
import com.health.assistant.vo.AuthTokenVO;
import com.health.assistant.vo.UserVO;

public interface AuthService {

    AuthTokenVO register(RegisterRequest request, ClientInfo clientInfo);

    AuthTokenVO login(LoginRequest request, ClientInfo clientInfo);

    AuthTokenVO refresh(String refreshToken, ClientInfo clientInfo);

    void logout(Long userId, String refreshToken);

    UserVO getCurrentUser(Long userId);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserVO updateProfile(Long userId, UpdateProfileRequest request);
}
