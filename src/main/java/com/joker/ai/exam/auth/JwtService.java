package com.joker.ai.exam.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * access token 的签发与校验。refresh token 不走 JWT，存 Redis 以便即时吊销。
 */
@Slf4j
@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        String secret = properties.getSecret();
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException("""
                    app.jwt.secret 缺失或短于 32 字节，无法签发 JWT。
                    请复制 application-local.yml.example 为 application-local.yml 并填入 app.jwt.secret，
                    或设置环境变量 JWT_SECRET（该文件已在 .gitignore 中，勿提交真实密钥）。""");
        }
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + properties.getAccessTtlSeconds() * 1000))
                .signWith(key)
                .compact();
    }

    /** token 无效/过期时返回 null，由调用方决定按未登录处理 */
    public AuthPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = claims.get(CLAIM_USER_ID, Number.class).longValue();
            String role = claims.get(CLAIM_ROLE, String.class);
            return new AuthPrincipal(userId, claims.getSubject(), role);
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            log.debug("JWT 校验失败：{}", e.getMessage());
            return null;
        }
    }

    public long getAccessTtlSeconds() {
        return properties.getAccessTtlSeconds();
    }

    public record AuthPrincipal(Long userId, String username, String role) {
    }
}
