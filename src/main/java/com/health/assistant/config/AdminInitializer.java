package com.health.assistant.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.common.constant.RoleConstants;
import com.health.assistant.entity.SysUser;
import com.health.assistant.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-password:admin123456}")
    private String defaultPassword;

    @Override
    public void run(ApplicationArguments args) {
        Long adminCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getRole, RoleConstants.ADMIN));
        if (adminCount != null && adminCount > 0) {
            return;
        }
        SysUser existing = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, ADMIN_USERNAME));
        if (existing != null) {
            existing.setRole(RoleConstants.ADMIN);
            existing.setPassword(passwordEncoder.encode(defaultPassword));
            existing.setStatus(1);
            userMapper.updateById(existing);
            log.warn("promoted existing user '{}' to ADMIN with default password", ADMIN_USERNAME);
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(defaultPassword));
        admin.setNickname("管理员");
        admin.setRole(RoleConstants.ADMIN);
        admin.setStatus(1);
        userMapper.insert(admin);
        log.warn("created default admin account '{}' (please change the password)", ADMIN_USERNAME);
    }
}
