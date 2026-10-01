package com.joker.ai.exam.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 签名密钥，至少 32 字节；只允许来自环境变量或 application-local.yml */
    private String secret = "";

    private String issuer = "exam-system-online";

    /** access token 有效期（秒） */
    private long accessTtlSeconds = 7200;

    /** refresh token 有效期（秒），同时作为 Redis 里的存活期 */
    private long refreshTtlSeconds = 604800;
}
