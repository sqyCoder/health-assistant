package com.health.assistant.controller;

import com.health.assistant.common.api.Result;
import com.health.assistant.common.web.ClientInfo;
import com.health.assistant.dto.auth.ChangePasswordRequest;
import com.health.assistant.dto.auth.LoginRequest;
import com.health.assistant.dto.auth.RefreshTokenRequest;
import com.health.assistant.dto.auth.RegisterRequest;
import com.health.assistant.dto.auth.UpdateProfileRequest;
import com.health.assistant.security.SecurityUtils;
import com.health.assistant.service.AuthService;
import com.health.assistant.vo.AuthTokenVO;
import com.health.assistant.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册并自动登录")
    @PostMapping("/register")
    public Result<AuthTokenVO> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.register(request, ClientInfo.from(httpRequest)));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<AuthTokenVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, ClientInfo.from(httpRequest)));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<AuthTokenVO> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.refresh(request.getRefreshToken(), ClientInfo.from(httpRequest)));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(SecurityUtils.getCurrentUserId(), request == null ? null : request.getRefreshToken());
        return Result.success();
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.getCurrentUser(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return Result.success();
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(authService.updateProfile(SecurityUtils.getCurrentUserId(), request));
    }
}
