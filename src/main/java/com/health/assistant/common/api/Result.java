package com.health.assistant.common.api;

import com.health.assistant.common.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;

@Getter
@Setter
public class Result<T> {

    public static final int SUCCESS_CODE = 0;

    private int code;
    private String message;
    private T data;
    private String traceId;

    public Result() {
        this.traceId = MDC.get("traceId");
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(SUCCESS_CODE);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
