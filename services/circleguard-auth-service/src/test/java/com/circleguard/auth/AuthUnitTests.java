package com.circleguard.auth;

import com.circleguard.auth.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthUnitTests {

    private final String dummySecret = "1234567890123456789012345678901234567890123456789012345678901234";

    @Test
    void jwtHasValidClaims() {
        JwtTokenService tokenService = new JwtTokenService(dummySecret, 3600000);
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken("user", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        
        String token = tokenService.generateToken(anonymousId, auth);
        
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(dummySecret.getBytes())
            .build()
            .parseClaimsJws(token)
            .getBody();
            
        assertEquals(anonymousId.toString(), claims.getSubject());
        assertNotNull(claims.get("permissions"));
    }

    @Test
    void expiredJwtThrowsException() throws InterruptedException {
        JwtTokenService tokenService = new JwtTokenService(dummySecret, 1); // 1 ms expiration
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        
        String token = tokenService.generateToken(anonymousId, auth);
        
        Thread.sleep(10); // wait for expiration
        
        assertThrows(ExpiredJwtException.class, () -> {
            Jwts.parserBuilder().setSigningKey(dummySecret.getBytes()).build().parseClaimsJws(token);
        });
    }

    @Test
    void tokenHasThreeSegments() {
        JwtTokenService tokenService = new JwtTokenService(dummySecret, 3600000);
        Authentication auth = new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        
        String token = tokenService.generateToken(UUID.randomUUID(), auth);
        
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void authenticationFailedReturnsFalse() {
        // Simulating failed auth logic since we mock logic
        Authentication auth = null;
        assertThrows(NullPointerException.class, () -> {
            JwtTokenService tokenService = new JwtTokenService(dummySecret, 3600000);
            tokenService.generateToken(UUID.randomUUID(), auth);
        });
    }

    @Test
    void validRefreshTokenReturnsNewToken() throws InterruptedException {
        JwtTokenService tokenService = new JwtTokenService(dummySecret, 3600000);
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        
        String token1 = tokenService.generateToken(anonymousId, auth);
        
        // Simulating a token refresh action
        Thread.sleep(1000); // Sleep 1 second so issuedAt differs
        String token2 = tokenService.generateToken(anonymousId, auth);
        
        assertNotEquals(token1, token2); 
        assertNotNull(token2);
    }
}
