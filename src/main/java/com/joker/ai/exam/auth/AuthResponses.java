package com.joker.ai.exam.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joker.ai.exam.common.ErrorCode;
import com.joker.ai.exam.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

/**
 * 鉴权失败也要回后端统一的 Result 结构，只是这里保留真实 HTTP 状态（401/403），
 * 方便前端拦截器区分「未登录」与「业务错误」。
 */
final class AuthResponses {

    @SneakyThrows
    static void write(ObjectMapper mapper, HttpServletResponse response, int httpStatus, ErrorCode errorCode) {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(mapper.writeValueAsString(
                Result.error(errorCode.getCode(), errorCode.getMessage())));
    }

    private AuthResponses() {
    }
}
