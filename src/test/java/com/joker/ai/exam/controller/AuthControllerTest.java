package com.joker.ai.exam.controller;

import com.joker.ai.exam.auth.AuthUserDetailsService;
import com.joker.ai.exam.auth.JwtService;
import com.joker.ai.exam.common.BizException;
import com.joker.ai.exam.common.ErrorCode;
import com.joker.ai.exam.config.GlobalExceptionHandler;
import com.joker.ai.exam.service.AuthService;
import com.joker.ai.exam.vo.LoginResponseVo;
import com.joker.ai.exam.vo.UserProfileVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                // standalone 模式不会自动装配安全参数解析器，@AuthenticationPrincipal 需显式注册
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void loginReturnsTokenPair() throws Exception {
        LoginResponseVo vo = new LoginResponseVo();
        vo.setUserId(1L);
        vo.setUsername("admin");
        vo.setRole("ADMIN");
        vo.setToken("access-token");
        vo.setRefreshToken("refresh-token");
        vo.setExpiresIn(7200);
        when(authService.login(any())).thenReturn(vo);

        mockMvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void loginRejectsBlankUsername() throws Exception {
        mockMvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void badCredentialsMapsToUnauthorizedCode() throws Exception {
        when(authService.login(any())).thenThrow(new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));

        mockMvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void refreshRequiresToken() throws Exception {
        mockMvc().perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void meWithoutPrincipalIsUnauthorized() throws Exception {
        mockMvc().perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void meWithPrincipalReturnsProfile() throws Exception {
        UserProfileVo profile = new UserProfileVo();
        profile.setUserId(9L);
        profile.setUsername("student01");
        profile.setRole("STUDENT");
        when(authService.profile(anyLong())).thenReturn(profile);
        JwtService.AuthPrincipal principal = new JwtService.AuthPrincipal(9L, "student01", "STUDENT");
        // 真实解析路径：AuthenticationPrincipalArgumentResolver 从 SecurityContextHolder 取认证信息
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        AuthUserDetailsService.toAuthorities("STUDENT")));
        try {
            mockMvc().perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(9))
                    .andExpect(jsonPath("$.data.role").value("STUDENT"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void logoutWithoutBodyPassesNullTokenToService() throws Exception {
        mockMvc().perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).logout(null);
    }
}
