package com.joker.ai.exam.controller;


import com.joker.ai.exam.auth.JwtService;
import com.joker.ai.exam.common.Result;
import com.joker.ai.exam.service.AuthService;
import com.joker.ai.exam.vo.LoginRequestVo;
import com.joker.ai.exam.vo.LoginResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


/**
 * 用户控制器 - 处理用户认证和权限管理相关的HTTP请求
 * 登录已迁到 AuthService；/api/auth/login 是本接口的正式版本，此处保留兼容旧前端调用。
 */
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/user")  // 用户API路径前缀
@Tag(name = "用户管理", description = "用户相关操作，包括登录认证、权限验证等功能")  // Swagger API分组
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /**
     * 用户登录
     * @param loginRequestVo 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")  // 处理POST请求
    @Operation(summary = "用户登录", description = "用户通过用户名和密码进行登录验证，返回用户信息和token")  // API描述
    public Result<LoginResponseVo> login(@Valid @RequestBody LoginRequestVo loginRequestVo) {
        return Result.success(authService.login(loginRequestVo));
    }

    /**
     * 检查用户权限
     * @param userId 用户ID
     * @return 权限检查结果
     */
    @GetMapping("/check-admin/{userId}")  // 处理GET请求
    @Operation(summary = "检查管理员权限", description = "验证指定用户是否具有管理员权限")  // API描述
    public Result<Boolean> checkAdmin(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @AuthenticationPrincipal JwtService.AuthPrincipal principal) {

        boolean isAdmin = principal != null
                && principal.userId().equals(userId)
                && "ADMIN".equals(principal.role());
        return Result.success(isAdmin);
    }
}
