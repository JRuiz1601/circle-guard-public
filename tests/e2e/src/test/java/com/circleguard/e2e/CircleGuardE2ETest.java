package com.circleguard.e2e;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static io.restassured.RestAssured.given;

/**
 * Requiere los microservicios accesibles en localhost (por ejemplo con port-forward a circleguard-dev):
 * <pre>
 *   kubectl port-forward -n circleguard-dev svc/circleguard-auth-service 8081:80
 *   kubectl port-forward -n circleguard-dev svc/circleguard-identity-service 8082:80
 *   kubectl port-forward -n circleguard-dev svc/circleguard-form-service 8083:80
 *   kubectl port-forward -n circleguard-dev svc/circleguard-file-service 8084:80
 *   kubectl port-forward -n circleguard-dev svc/circleguard-dashboard-service 8085:80
 * </pre>
 * Puertos por defecto; se pueden sobrescribir con variables de entorno {@code CIRCLEGUARD_E2E_*_BASE}.
 * <p>Ejecución en un solo hilo para no saturar los mismos port-forward con llamadas concurrentes.</p>
 */
@Tag("e2e")
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CircleGuardE2ETest {

    private static String authBase;
    private static String identityBase;
    private static String formBase;
    private static String fileBase;
    private static String dashboardBase;

    @BeforeAll
    static void setup() {
        authBase = env("CIRCLEGUARD_E2E_AUTH_BASE", "http://localhost:8081");
        identityBase = env("CIRCLEGUARD_E2E_IDENTITY_BASE", "http://localhost:8082");
        formBase = env("CIRCLEGUARD_E2E_FORM_BASE", "http://localhost:8083");
        fileBase = env("CIRCLEGUARD_E2E_FILE_BASE", "http://localhost:8084");
        dashboardBase = env("CIRCLEGUARD_E2E_DASHBOARD_BASE", "http://localhost:8085");
        RestAssured.config = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 120_000)
                        .setParam("http.socket.timeout", 120_000));
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v.trim();
    }

    /** Usuario sembrado en Flyway V2__seed_test_users.sql (password: {@code password}). */
    private String loginAsHealthUser() {
        return given()
                .baseUri(authBase)
                .contentType(ContentType.JSON)
                .body(Map.of("username", "health_user", "password", "password"))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    @Test
    @Order(1)
    void uploadFile() {
        String token = loginAsHealthUser();
        given()
                .baseUri(fileBase)
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "test.txt", "hello".getBytes())
                .when()
                .post("/api/v1/files/upload")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(2)
    void createAndListQuestionnaires() {
        String token = loginAsHealthUser();
        String title = "e2e-q-" + UUID.randomUUID();

        given()
                .baseUri(formBase)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of(
                        "title", title,
                        "description", "e2e",
                        "version", 1,
                        "isActive", false))
                .when()
                .post("/api/v1/questionnaires")
                .then()
                .statusCode(200);

        given()
                .baseUri(formBase)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/questionnaires")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    void loginThenLookupWithJwt() {
        String token = loginAsHealthUser();
        Object aid = given()
                .baseUri(identityBase)
                .contentType(ContentType.JSON)
                .body(Map.of("realIdentity", "e2e|subject|" + UUID.randomUUID()))
                .when()
                .post("/api/v1/identities/map")
                .then()
                .statusCode(200)
                .extract()
                .path("anonymousId");
        String anonymousIdStr = aid != null ? aid.toString() : "";

        given()
                .baseUri(identityBase)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/identities/lookup/" + anonymousIdStr)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(4)
    void unauthenticatedLookupReturns401() {
        given()
                .baseUri(identityBase)
                .when()
                .get("/api/v1/identities/lookup/" + UUID.randomUUID())
                .then()
                .statusCode(401);
    }

    /** Tablero de salud agregado ({@code AnalyticsController}); sin seguridad en dashboard-service. */
    @Test
    @Order(5)
    void dashboardHealthBoardReturnsOk() {
        given()
                .baseUri(dashboardBase)
                .when()
                .get("/api/v1/analytics/health-board")
                .then()
                .statusCode(200);
    }
}
