package com.health.assistant.service.impl;

import com.health.assistant.common.exception.AuthException;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.common.util.HashUtils;
import com.health.assistant.common.web.ClientInfo;
import com.health.assistant.dto.auth.LoginRequest;
import com.health.assistant.dto.auth.RegisterRequest;
import com.health.assistant.entity.SysUser;
import com.health.assistant.entity.SysUserToken;
import com.health.assistant.mapper.SysUserMapper;
import com.health.assistant.mapper.SysUserTokenMapper;
import com.health.assistant.security.JwtProperties;
import com.health.assistant.security.JwtTokenProvider;
import com.health.assistant.service.AuditService;
import com.health.assistant.support.LoginRateLimiter;
import com.health.assistant.vo.AuthTokenVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final ClientInfo CLIENT = new ClientInfo("127.0.0.1", "junit-agent");

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysUserTokenMapper tokenMapper;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private AuditService auditService;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("unit-test-secret-key-0123456789abcdef");
        jwtProperties.setAccessExpireMinutes(120);
        jwtProperties.setRefreshExpireDays(7);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        authService = new AuthServiceImpl(userMapper, tokenMapper, passwordEncoder,
                jwtTokenProvider, jwtProperties, loginRateLimiter, auditService);
    }

    @Test
    void registerShouldEncodePasswordAndIssueTokens() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SysUser.class).setId(1001L);
            return 1;
        });
        when(tokenMapper.insert(any(SysUserToken.class))).thenReturn(1);

        RegisterRequest request = buildRegisterRequest("alice", "passw0rd123", "Alice");
        AuthTokenVO result = authService.register(request, CLIENT);

        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(result.getRefreshToken()).isNotBlank();
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresInSeconds()).isEqualTo(7200);
        assertThat(result.getUser().getId()).isEqualTo(1001L);
        assertThat(result.getUser().getUsername()).isEqualTo("alice");

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(userCaptor.capture());
        SysUser saved = userCaptor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo("passw0rd123");
        assertThat(passwordEncoder.matches("passw0rd123", saved.getPassword())).isTrue();
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.getStatus()).isEqualTo(1);

        verify(tokenMapper).insert(any(SysUserToken.class));
        verify(auditService).record(eq(1001L), eq("alice"), eq("REGISTER"), eq("USER"), eq(1001L), eq(CLIENT));
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        RegisterRequest request = buildRegisterRequest("alice", "passw0rd123", "Alice");

        assertThatThrownBy(() -> authService.register(request, CLIENT))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.USERNAME_EXISTS.getCode()));

        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void loginShouldIssueTokensOnSuccess() {
        SysUser user = buildUser(2002L, "bob", "passw0rd123");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
        when(tokenMapper.insert(any(SysUserToken.class))).thenReturn(1);

        LoginRequest request = buildLoginRequest("bob", "passw0rd123");
        AuthTokenVO result = authService.login(request, CLIENT);

        assertThat(result.getUser().getId()).isEqualTo(2002L);
        assertThat(result.getAccessToken()).isNotBlank();
        verify(loginRateLimiter).reset("bob", "127.0.0.1");
        verify(loginRateLimiter, never()).recordFailure(any(), any());
        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(user.getLastLoginIp()).isEqualTo("127.0.0.1");
    }

    @Test
    void loginShouldRejectWrongPasswordAndCountFailure() {
        SysUser user = buildUser(2002L, "bob", "passw0rd123");
        when(userMapper.selectOne(any())).thenReturn(user);

        LoginRequest request = buildLoginRequest("bob", "wrong-pass1");

        assertThatThrownBy(() -> authService.login(request, CLIENT))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.BAD_CREDENTIALS.getCode()));

        verify(loginRateLimiter).recordFailure("bob", "127.0.0.1");
        verify(tokenMapper, never()).insert(any(SysUserToken.class));
    }

    @Test
    void loginShouldRejectUnknownUser() {
        when(userMapper.selectOne(any())).thenReturn(null);

        LoginRequest request = buildLoginRequest("ghost", "passw0rd123");

        assertThatThrownBy(() -> authService.login(request, CLIENT))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.BAD_CREDENTIALS.getCode()));
    }

    @Test
    void loginShouldRejectDisabledUser() {
        SysUser user = buildUser(2002L, "bob", "passw0rd123");
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);

        LoginRequest request = buildLoginRequest("bob", "passw0rd123");

        assertThatThrownBy(() -> authService.login(request, CLIENT))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.USER_DISABLED.getCode()));
    }

    @Test
    void refreshShouldRotateTokenAndIssueNewPair() {
        String refreshToken = jwtTokenProvider.createRefreshToken(2002L);
        SysUser user = buildUser(2002L, "bob", "passw0rd123");
        SysUserToken stored = new SysUserToken();
        stored.setId(9L);
        stored.setUserId(2002L);
        stored.setTokenHash(HashUtils.sha256Hex(refreshToken));
        stored.setRevoked(0);
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(tokenMapper.selectOne(any())).thenReturn(stored);
        when(userMapper.selectById(2002L)).thenReturn(user);
        when(tokenMapper.updateById(any(SysUserToken.class))).thenReturn(1);
        when(tokenMapper.insert(any(SysUserToken.class))).thenReturn(1);

        AuthTokenVO result = authService.refresh(refreshToken, CLIENT);

        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(result.getRefreshToken()).isNotBlank();
        assertThat(stored.getRevoked()).isEqualTo(1);
        verify(tokenMapper).updateById(stored);
        verify(tokenMapper).insert(any(SysUserToken.class));
    }

    @Test
    void refreshShouldRejectRevokedToken() {
        String refreshToken = jwtTokenProvider.createRefreshToken(2002L);
        SysUserToken stored = new SysUserToken();
        stored.setTokenHash(HashUtils.sha256Hex(refreshToken));
        stored.setRevoked(1);
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(tokenMapper.selectOne(any())).thenReturn(stored);

        assertThatThrownBy(() -> authService.refresh(refreshToken, CLIENT))
                .isInstanceOfSatisfying(AuthException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID.getCode()));
    }

    @Test
    void refreshShouldRejectGarbageToken() {
        assertThatThrownBy(() -> authService.refresh("not-a-jwt", CLIENT))
                .isInstanceOfSatisfying(AuthException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID.getCode()));
    }

    private RegisterRequest buildRegisterRequest(String username, String password, String nickname) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setNickname(nickname);
        return request;
    }

    private LoginRequest buildLoginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private SysUser buildUser(Long id, String username, String rawPassword) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(username);
        user.setRole("USER");
        user.setStatus(1);
        return user;
    }
}
