package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.common.constant.AuditActions;
import com.health.assistant.common.constant.RoleConstants;
import com.health.assistant.common.exception.AuthException;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.common.util.HashUtils;
import com.health.assistant.common.web.ClientInfo;
import com.health.assistant.dto.auth.ChangePasswordRequest;
import com.health.assistant.dto.auth.LoginRequest;
import com.health.assistant.dto.auth.RegisterRequest;
import com.health.assistant.dto.auth.UpdateProfileRequest;
import com.health.assistant.entity.SysUser;
import com.health.assistant.entity.SysUserToken;
import com.health.assistant.mapper.SysUserMapper;
import com.health.assistant.mapper.SysUserTokenMapper;
import com.health.assistant.security.JwtProperties;
import com.health.assistant.security.JwtTokenProvider;
import com.health.assistant.service.AuditService;
import com.health.assistant.service.AuthService;
import com.health.assistant.support.LoginRateLimiter;
import com.health.assistant.vo.AuthTokenVO;
import com.health.assistant.vo.UserVO;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_CLIENT_INFO_LENGTH = 200;
    private static final int STATUS_ENABLED = 1;
    private static final int REVOKED = 1;

    private final SysUserMapper userMapper;
    private final SysUserTokenMapper tokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final LoginRateLimiter loginRateLimiter;
    private final AuditService auditService;

    @Value("${app.registration.max-users:10}")
    private int maxUsers;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO register(RegisterRequest request, ClientInfo clientInfo) {
        if (maxUsers > 0) {
            Long total = userMapper.selectCount(null);
            if (total != null && total >= maxUsers) {
                throw new BizException(ErrorCode.REGISTRATION_FULL,
                        "注册名额已满（上限 " + maxUsers + " 人），请联系管理员");
            }
        }
        Long exists = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole(RoleConstants.USER);
        user.setStatus(STATUS_ENABLED);
        userMapper.insert(user);
        auditService.record(user.getId(), user.getUsername(), AuditActions.REGISTER, "USER", user.getId(), clientInfo);
        return issueTokens(user, clientInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO login(LoginRequest request, ClientInfo clientInfo) {
        loginRateLimiter.check(request.getUsername(), clientInfo.ip());
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimiter.recordFailure(request.getUsername(), clientInfo.ip());
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != STATUS_ENABLED) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        loginRateLimiter.reset(request.getUsername(), clientInfo.ip());
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(clientInfo.ip());
        userMapper.updateById(user);
        auditService.record(user.getId(), user.getUsername(), AuditActions.LOGIN, "USER", user.getId(), clientInfo);
        return issueTokens(user, clientInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO refresh(String refreshToken, ClientInfo clientInfo) {
        Long userId;
        try {
            userId = jwtTokenProvider.parseRefreshToken(refreshToken);
        } catch (JwtException | BizException e) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        SysUserToken stored = tokenMapper.selectOne(
                Wrappers.<SysUserToken>lambdaQuery().eq(SysUserToken::getTokenHash, HashUtils.sha256Hex(refreshToken)));
        if (stored == null
                || Integer.valueOf(REVOKED).equals(stored.getRevoked())
                || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != STATUS_ENABLED) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        stored.setRevoked(REVOKED);
        tokenMapper.updateById(stored);
        return issueTokens(user, clientInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(Long userId, String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            tokenMapper.update(null, Wrappers.<SysUserToken>lambdaUpdate()
                    .eq(SysUserToken::getTokenHash, HashUtils.sha256Hex(refreshToken))
                    .eq(SysUserToken::getUserId, userId)
                    .set(SysUserToken::getRevoked, REVOKED));
        }
        SysUser user = userMapper.selectById(userId);
        auditService.record(userId, user == null ? null : user.getUsername(),
                AuditActions.LOGOUT, "USER", userId, null);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return UserVO.from(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        tokenMapper.update(null, Wrappers.<SysUserToken>lambdaUpdate()
                .eq(SysUserToken::getUserId, userId)
                .set(SysUserToken::getRevoked, REVOKED));
        auditService.record(userId, user.getUsername(), AuditActions.CHANGE_PASSWORD, "USER", userId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setNickname(request.getNickname());
        user.setAvatarUrl(request.getAvatarUrl());
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    private AuthTokenVO issueTokens(SysUser user, ClientInfo clientInfo) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        SysUserToken token = new SysUserToken();
        token.setUserId(user.getId());
        token.setTokenHash(HashUtils.sha256Hex(refreshToken));
        token.setExpiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshExpireDays()));
        token.setRevoked(0);
        token.setClientInfo(truncate(clientInfo == null ? null : clientInfo.userAgent(), MAX_CLIENT_INFO_LENGTH));
        tokenMapper.insert(token);

        return AuthTokenVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtProperties.getAccessExpireMinutes() * 60)
                .user(UserVO.from(user))
                .build();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
