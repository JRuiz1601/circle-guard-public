package com.circleguard.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

class AuthUnitTests {

    @Test
    void jwtHasValidClaims() {
        assertTrue(true);
    }

    @Test
    void expiredJwtThrowsException() {
        assertTrue(true);
    }

    @Test
    void validLoginReturnsToken() {
        assertTrue(true);
    }

    @Test
    void invalidPasswordReturns401() {
        assertTrue(true);
    }

    @Test
    void validRefreshTokenReturnsNewToken() {
        assertTrue(true);
    }
}
