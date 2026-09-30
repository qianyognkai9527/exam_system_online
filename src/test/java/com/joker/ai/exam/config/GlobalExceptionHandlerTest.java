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
}
