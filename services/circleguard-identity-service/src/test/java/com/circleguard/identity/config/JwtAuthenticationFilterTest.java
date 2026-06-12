package com.circleguard.identity.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposesOnly1234567890ABCDEF";

    private JwtAuthenticationFilter filter;
    private Key key;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesSubjectAndPermissions() throws Exception {
        String subject = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .setSubject(subject)
                .claim("permissions", List.of("identity:lookup", "audit:read"))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        doFilter("Bearer " + token);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(subject, auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("identity:lookup")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("audit:read")));
    }

    @Test
    void tokenWithoutPermissionsStillAuthenticatesSubject() throws Exception {
        String subject = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .setSubject(subject)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        doFilter("Bearer " + token);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(subject, auth.getName());
        assertTrue(auth.getAuthorities().isEmpty());
    }

    @Test
    void nonBearerAuthorizationHeaderLeavesContextEmpty() throws Exception {
        doFilter("Basic abc123");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void missingAuthorizationHeaderLeavesContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidTokenClearsExistingAuthentication() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("stale", null));

        doFilter("Bearer invalid.token.value");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private void doFilter(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorizationHeader);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
    }
}
