package com.health.assistant.security;

import com.health.assistant.common.exception.AuthException;
import com.health.assistant.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long getCurrentUserId() {
        return getLoginUser().id();
    }

    public static boolean isCurrentUserAdmin() {
        return "ADMIN".equals(getLoginUser().role());
    }
}
