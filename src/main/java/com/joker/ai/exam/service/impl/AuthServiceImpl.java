package com.joker.ai.exam.service.impl;

import com.joker.ai.exam.auth.AuthUserDetailsService;
import com.joker.ai.exam.auth.JwtService;
import com.joker.ai.exam.auth.JwtProperties;
import com.joker.ai.exam.common.BizException;
import com.joker.ai.exam.common.CacheConstants;
import com.joker.ai.exam.common.ErrorCode;
import com.joker.ai.exam.entity.User;
import com.joker.ai.exam.mapper.UsersMapper;
import com.joker.ai.exam.service.AuthService;
import com.joker.ai.exam.vo.LoginRequestVo;
import com.joker.ai.exam.vo.LoginResponseVo;
import com.joker.ai.exam.vo.UserProfileVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUserDetailsService userDetailsService;
    private final UsersMapper usersMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginResponseVo login(LoginRequestVo request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (DisabledException e) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已停用");
        } catch (BadCredentialsException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        } catch (AuthenticationException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        User user = userDetailsService.findByUsername(authentication.getName());
        return issue(user);
    }

    @Override
    public LoginResponseVo refresh(String refreshToken) {
        String key = CacheConstants.AUTH_REFRESH_KEY + refreshToken;
        String userId = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(userId)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        // 轮换：旧令牌用过一次即作废
        redisTemplate.delete(key);
        User user = usersMapper.selectById(Long.valueOf(userId));
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return issue(user);
    }

    @Override
    public void logout(String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            redisTemplate.delete(CacheConstants.AUTH_REFRESH_KEY + refreshToken);
        }
    }

    @Override
    public UserProfileVo profile(Long userId) {
        User user = usersMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        UserProfileVo vo = new UserProfileVo();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(normalizeRole(user.getRole()));
        return vo;
    }

    private LoginResponseVo issue(User user) {
        String role = normalizeRole(user.getRole());
        String accessToken = jwtService.createAccessToken(user.getId(), user.getUsername(), role);
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(CacheConstants.AUTH_REFRESH_KEY + refreshToken,
                String.valueOf(user.getId()), Duration.ofSeconds(jwtProperties.getRefreshTtlSeconds()));

        LoginResponseVo vo = new LoginResponseVo();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(role);
        vo.setToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(jwtProperties.getAccessTtlSeconds());
        return vo;
    }

    /** 历史数据里 role 存过小写 admin，统一成大写枚举 */
    private String normalizeRole(String role) {
        return StringUtils.hasText(role) ? role.trim().toUpperCase() : "STUDENT";
    }
}
