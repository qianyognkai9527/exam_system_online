package com.joker.ai.exam.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joker.ai.exam.entity.User;
import com.joker.ai.exam.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 登录校验用（每个请求的身份来自 JWT claim，不再回查数据库）。
 */
@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final UsersMapper usersMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = usersMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        // 库里 role 历史上存过小写 admin，统一归一
        String role = user.getRole() == null ? "STUDENT" : user.getRole().trim().toUpperCase();
        boolean enabled = !"disabled".equalsIgnoreCase(user.getStatus());
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword() == null ? "" : user.getPassword())
                .roles(role)
                .disabled(!enabled)
                .build();
    }

    public User findByUsername(String username) {
        return usersMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
    }

    public static List<SimpleGrantedAuthority> toAuthorities(String role) {
        String normalized = role == null || role.isBlank() ? "STUDENT" : role.trim().toUpperCase();
        return List.of(new SimpleGrantedAuthority("ROLE_" + normalized));
    }
}
