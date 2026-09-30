package com.joker.ai.exam.config;

import com.joker.ai.exam.common.BizException;
import com.joker.ai.exam.common.ErrorCode;
import com.joker.ai.exam.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getErrorCode().getCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleValidationException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : ErrorCode.PARAM_ERROR.getMessage();
        log.warn("参数校验失败：{}", message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFoundException(NoResourceFoundException e) {
        log.warn("资源不存在：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException e) {
        log.warn("缺少请求参数：{}", e.getParameterName());
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), "缺少请求参数：" + e.getParameterName());
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持：{}", e.getMessage());
        return Result.error(ErrorCode.METHOD_NOT_ALLOWED.getCode(), ErrorCode.METHOD_NOT_ALLOWED.getMessage());
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaTypeNotSupported(org.springframework.web.HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的媒体类型：{}", e.getMessage());
        return Result.error(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode(), ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage());
    }
}
