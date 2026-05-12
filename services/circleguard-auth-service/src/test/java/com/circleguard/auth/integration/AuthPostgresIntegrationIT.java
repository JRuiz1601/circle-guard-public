package com.circleguard.auth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración MockMvc + PostgreSQL (Testcontainers) + stub HTTP identity (MockWebServer).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthPostgresIntegrationIT {

    private static final String STUB_ANONYMOUS_ID = "550e8400-e29b-41d4-a716-446655440000";

    private static final MockWebServer IDENTITY_MOCK = new MockWebServer();

    static {
        try {
            IDENTITY_MOCK.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("circleguard_auth")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("jwt.secret", () -> "my-super-secret-test-key-32-chars-long-12345678");
        registry.add("jwt.expiration", () -> "3600000");
        registry.add("qr.secret", () -> "ThisIsAQRSecretKeyForTestingOnly1234567890");
        registry.add("qr.expiration", () -> "300");
        registry.add("circleguard.identity.map-url",
                () -> "http://localhost:" + IDENTITY_MOCK.getPort() + "/api/v1/identities/map");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LdapAuthenticationProvider ldapAuthenticationProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterAll
    static void shutdownIdentityMock() throws IOException {
        IDENTITY_MOCK.shutdown();
    }

    @BeforeEach
    void ldapFailsFastSoLocalDbAuthRuns() {
        when(ldapAuthenticationProvider.authenticate(any()))
                .thenThrow(new BadCredentialsException("ldap skipped in integration test"));
        IDENTITY_MOCK.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("/api/v1/identities/map".equals(request.getPath())) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody("{\"anonymousId\":\"" + STUB_ANONYMOUS_ID + "\"}");
                }
                return new MockResponse().setResponseCode(404);
            }
        });
    }

    @Test
    void loginCallsIdentityStubAndReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"health_user\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.anonymousId").value(STUB_ANONYMOUS_ID))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    void validJwtAllowsQrGenerateEndpoint() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"health_user\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = body.get("token").asText();

        mockMvc.perform(get("/api/v1/auth/qr/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrToken").exists())
                .andExpect(jsonPath("$.expiresIn").exists());
    }
}
