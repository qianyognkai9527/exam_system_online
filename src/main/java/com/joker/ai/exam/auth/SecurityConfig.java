package com.joker.ai.exam.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 无状态 JWT 鉴权。P2 有意保持「URL 规则集中在一处」，避免各控制器散落注解导致漏配；
 * 破坏性写操作另加 @PreAuthorize 做纵深防御。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** 无需登录即可访问的只读接口（仅计划 §7 授权的前台展示类：视频/轮播/公告/试卷列表/分类） */
    private static final String[] PUBLIC_GET = {
            "/api/videos", "/api/videos/**",
            "/api/video-categories", "/api/video-categories/**",
            "/api/banners/active", "/api/banners/list",
            "/api/notices/active", "/api/notices/latest",
            "/api/papers/list",
            "/api/categories", "/api/categories/tree",
    };

    /** 匿名可触发的写操作：视频播放时长与点赞按 IP 计数，不需要登录 */
    private static final String[] PUBLIC_POST = {
            "/api/videos/*/view", "/api/videos/*/like",
    };

    private static final String[] PUBLIC_ALWAYS = {
            "/api/auth/**", "/api/user/login",
            "/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/swagger-resources/**", "/favicon.ico", "/error", "/actuator/**", "/files/**",
    };

    @Value("${app.cors.allowed-origins:http://localhost:3001}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                          JwtAuthenticationFilter jwtFilter,
                                          ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ALWAYS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        // 学生本人也要能考完交卷、查自己的成绩
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 排行榜登录即可看；成绩列表/详情只在管理台用，学生登录也不该看到别人的分数
                        .requestMatchers("/api/exam-records/ranking").authenticated()
                        .requestMatchers("/api/exam-records/list", "/api/exam-records/*").hasRole("ADMIN")
                        .requestMatchers("/api/exams/**").authenticated()
                        // 用户投稿视频属于登录用户，不给 ADMIN
                        .requestMatchers("/api/videos/submit").authenticated()
                        // 其余一律先登录：题目/答案/成绩/统计都不再对匿名开放
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                AuthResponses.write(objectMapper, response, 401,
                                        com.joker.ai.exam.common.ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, e) ->
                                AuthResponses.write(objectMapper, response, 403,
                                        com.joker.ai.exam.common.ErrorCode.FORBIDDEN)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
