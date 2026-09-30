package com.joker.ai.exam.config;

import com.joker.ai.exam.common.BizException;
import com.joker.ai.exam.common.ErrorCode;
import com.joker.ai.exam.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBizException_returnsCodeAndMessage() {
        Result<Void> result = handler.handleBizException(new BizException(ErrorCode.NOT_FOUND, "试卷不存在"));
        assertEquals(404, result.getCode());
        assertEquals("试卷不存在", result.getMessage());
    }

    @Test
    void handleException_hidesInternalMessage() {
        Result<Void> result = handler.handleException(new IllegalStateException("db down"));
        assertEquals(500, result.getCode());
        assertEquals(ErrorCode.SYSTEM_ERROR.getMessage(), result.getMessage());
    }

    @Test
    void handleNotFoundException_returnsHttp404() {
        NoResourceFoundException ex = new NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/actuator/metrics");
        ResponseEntity<Result<Void>> response = handler.handleNotFoundException(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getCode());
    }

    @Test
    void handleValidationException_returns400WithFieldMessage() {
        org.springframework.validation.BindException ex =
                new org.springframework.validation.BindException(new Object(), "startExamVo");
        ex.addError(new org.springframework.validation.FieldError("startExamVo", "studentName", "考生姓名不能为空"));
        Result<Void> result = handler.handleValidationException(ex);
        assertEquals(400, result.getCode());
        assertEquals("考生姓名不能为空", result.getMessage());
    }

    @Test
    void handleMethodNotSupported_returns405() {
        Result<Void> result = handler.handleMethodNotSupported(
                new org.springframework.web.HttpRequestMethodNotSupportedException("POST"));
        assertEquals(405, result.getCode());
    }
}
