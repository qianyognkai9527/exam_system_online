package com.joker.ai.exam.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef-test-only-secret";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTtlSeconds(60);
        jwtService = new JwtService(properties);
        jwtService.init();
    }

    @Test
    void accessTokenRoundTripsPrincipalClaims() {
        String token = jwtService.createAccessToken(7L, "admin", "ADMIN");

        JwtService.AuthPrincipal principal = jwtService.parse(token);

        assertNotNull(principal);
        assertEquals(7L, principal.userId());
        assertEquals("admin", principal.username());
        assertEquals("ADMIN", principal.role());
    }

    @Test
    void rejectsGarbageAndForeignSignature() {
        assertNull(jwtService.parse("not.a.jwt"));
        assertNull(jwtService.parse("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.AAAA"));

        JwtProperties other = new JwtProperties();
        other.setSecret("ffffffffffffffffffffffffffffffff-different-secret");
        other.setAccessTtlSeconds(60);
        JwtService foreign = new JwtService(other);
        foreign.init();

        assertNull(jwtService.parse(foreign.createAccessToken(1L, "x", "STUDENT")));
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret(SECRET);
        shortLived.setAccessTtlSeconds(-1);
        JwtService service = new JwtService(shortLived);
        service.init();

        assertNull(service.parse(service.createAccessToken(1L, "admin", "ADMIN")));
    }

    @Test
    void refusesToStartWithWeakSecret() {
        JwtProperties weak = new JwtProperties();
        weak.setSecret("too-short");

        JwtService service = new JwtService(weak);

        IllegalStateException e = assertThrows(IllegalStateException.class, service::init);
        assertTrue(e.getMessage().contains("app.jwt.secret"));
    }
}
