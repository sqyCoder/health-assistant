package com.health.assistant.common.exception;

import com.health.assistant.common.api.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("biz exception: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ThirdPartyException.class)
    public Result<Void> handleThirdPartyException(ThirdPartyException e) {
        log.error("third party exception: code={}, message={}", e.getCode(), e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = ErrorCode.PARAM_ERROR.getMessage();
        }
        return ResponseEntity.badRequest().body(Result.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = ErrorCode.PARAM_ERROR.getMessage();
        }
        return ResponseEntity.badRequest().body(Result.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("message not readable: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Result.error(ErrorCode.PARAM_ERROR));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Result.error(ErrorCode.FILE_TOO_LARGE));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Result.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("unexpected exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(ErrorCode.SYSTEM_ERROR));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
