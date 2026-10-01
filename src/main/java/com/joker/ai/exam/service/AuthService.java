package com.joker.ai.exam.service;

import com.joker.ai.exam.vo.LoginRequestVo;
import com.joker.ai.exam.vo.LoginResponseVo;
import com.joker.ai.exam.vo.UserProfileVo;

public interface AuthService {

    LoginResponseVo login(LoginRequestVo request);

    /** 旧 refresh token 立即失效（轮换） */
    LoginResponseVo refresh(String refreshToken);

    void logout(String refreshToken);

    UserProfileVo profile(Long userId);
}
