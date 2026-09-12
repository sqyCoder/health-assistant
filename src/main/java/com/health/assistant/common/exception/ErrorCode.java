package com.health.assistant.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),

    USERNAME_EXISTS(10001, "用户名已存在"),
    BAD_CREDENTIALS(10002, "用户名或密码错误"),
    USER_DISABLED(10003, "账号已被禁用"),
    REFRESH_TOKEN_INVALID(10004, "登录状态已失效，请重新登录"),
    USER_NOT_FOUND(10005, "用户不存在"),
    LOGIN_RATE_LIMITED(10006, "登录尝试过于频繁，请 1 分钟后再试"),
    REGISTRATION_FULL(10007, "注册名额已满，请联系管理员"),

    MEMBER_NOT_FOUND(10101, "成员不存在或无权访问"),

    REPORT_NOT_FOUND(10201, "报告不存在或状态不允许操作"),
    PDF_PARSE_FAILED(10202, "PDF 解析失败"),
    PDF_SCANNED(10203, "检测到扫描版 PDF，请手动录入"),

    AI_GENERATE_FAILED(10301, "AI 生成失败，请稍后重试"),
    AI_FORMAT_ERROR(10302, "AI 返回格式异常，请重试"),
    AI_DAILY_LIMIT_EXCEEDED(10303, "今日 AI 调用次数已达上限，请明天再试"),

    CHAT_SESSION_NOT_FOUND(10401, "会话不存在或无权访问"),
    CHAT_SESSION_BUSY(10402, "当前会话正在生成回复，请稍候"),

    EXPORT_FAILED(10501, "导出失败"),

    TASK_NOT_FOUND(10601, "任务不存在或无权访问"),

    FILE_TYPE_NOT_ALLOWED(10901, "上传文件类型不允许"),
    FILE_TOO_LARGE(10902, "上传文件超过大小限制"),
    FILE_NOT_FOUND(10903, "文件不存在或已被删除"),

    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    FORBIDDEN(40300, "没有操作权限"),
    NOT_FOUND(40400, "资源不存在"),
    METHOD_NOT_ALLOWED(40500, "请求方法不支持"),

    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试"),

    DEEPSEEK_TIMEOUT(90101, "AI 服务响应超时，请稍后重试"),
    DEEPSEEK_RATE_LIMITED(90102, "AI 服务繁忙，请稍后重试"),
    DEEPSEEK_ERROR(90103, "AI 服务异常，请稍后重试"),
    STORAGE_ERROR(90201, "文件存储服务异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
