package com.health.assistant.common.exception;

import lombok.Getter;

@Getter
public class ThirdPartyException extends RuntimeException {

    private final int code;

    public ThirdPartyException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public ThirdPartyException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public ThirdPartyException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }
}
