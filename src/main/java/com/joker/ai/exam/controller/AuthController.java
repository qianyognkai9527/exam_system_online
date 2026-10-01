package com.joker.ai.exam.controller;

import com.joker.ai.exam.auth.JwtService;
import com.joker.ai.exam.common.BizException;
import com.joker.ai.exam.common.ErrorCode;
import com.joker.ai.exam.common.Result;
import com.joker.ai.exam.service.AuthService;
import com.joker.ai.exam.vo.LoginRequestVo;
import com.joker.ai.exam.vo.LoginResponseVo;
import com.joker.ai.exam.vo.RefreshTokenVo;
import com.joker.ai.exam.vo.UserProfileVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证鉴权", description = "登录、刷新、登出与当前用户")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "用户名密码换取 access token（JWT）与 refresh token（Redis）")
    @PostMapping("/login")
    public Result<LoginResponseVo> login(@Valid @RequestBody LoginRequestVo request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "刷新令牌", description = "旧 refresh token 用后即废，返回成对的新令牌")
    @PostMapping("/refresh")
    public Result<LoginResponseVo> refresh(@Valid @RequestBody RefreshTokenVo request) {
        return Result.success(authService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "登出", description = "吊销 refresh token；access token 随其自然过期")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody(required = false) RefreshTokenVo request) {
        authService.logout(request == null ? null : request.getRefreshToken());
        return Result.success("退出成功");
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<UserProfileVo> me(@AuthenticationPrincipal JwtService.AuthPrincipal principal) {
        if (principal == null) {
            // /api/auth/** 是放行路径，未带令牌时不会走到鉴权拦截，需要自己拒绝
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return Result.success(authService.profile(principal.userId()));
    }
}
