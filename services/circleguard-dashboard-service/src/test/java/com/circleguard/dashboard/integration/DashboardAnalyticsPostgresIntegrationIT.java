package com.circleguard.dashboard.integration;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc + PostgreSQL (Testcontainers) + stub promotion-service (MockWebServer) — datos tipo dashboard.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DashboardAnalyticsPostgresIntegrationIT {

    private static final MockWebServer PROMOTION_MOCK = new MockWebServer();

    static {
        try {
            PROMOTION_MOCK.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("circleguard_dashboard")
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
        registry.add("circleguard.promotion-service.url",
                () -> "http://localhost:" + PROMOTION_MOCK.getPort());
    }

    @Autowired
    private MockMvc mockMvc;

    @AfterAll
    static void shutdownPromotionMock() throws IOException {
        PROMOTION_MOCK.shutdown();
    }

    @BeforeEach
    void stubPromotionStats() {
        PROMOTION_MOCK.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("/api/v1/health-status/stats".equals(request.getPath())) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody("{\"totalSurveys\":0,\"symptomaticRate\":0}");
                }
                return new MockResponse().setResponseCode(404);
            }
        });
    }

    @Test
    void healthBoardAggregatesPromotionStub() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/health-board"))
                .andExpect(status().isOk());
    }
}
