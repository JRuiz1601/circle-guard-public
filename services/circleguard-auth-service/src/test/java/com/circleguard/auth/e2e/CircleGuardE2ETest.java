package com.circleguard.auth.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CircleGuardE2ETest {

    private static final String BASE_AUTH = "http://localhost:8081";
    private static final String BASE_FORM = "http://localhost:8083";
    private static String authToken;

    @Test
    @Order(1)
    @Disabled("E2E - requiere servicios corriendo en dev")
    void loginCompletoRetornaToken() {
        authToken = given()
                .contentType("application/json")
                .body("{\"username\":\"admin\",\"password\":\"admin\"}") // Credentials depending on DB state
                .when()
                .post(BASE_AUTH + "/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .extract().path("accessToken");
    }

    @Test
    @Order(2)
    @Disabled("E2E - requiere servicios corriendo en dev")
    void crearFormularioConTokenValido() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .contentType("application/json")
                .body("{\"title\":\"Formulario E2E\",\"description\":\"Form Test\",\"active\":true}")
                .when()
                .post(BASE_FORM + "/api/v1/questionnaires")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    @Disabled("E2E - requiere servicios corriendo en dev")
    void subirArchivoRetornaFilename() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .multiPart("file", "test.txt", "Dummy File Content".getBytes())
                .when()
                .post(BASE_FORM + "/api/v1/attachments")
                .then()
                .statusCode(200)
                .body("filename", notNullValue());
    }

    @Test
    @Order(4)
    @Disabled("E2E - requiere servicios corriendo en dev")
    void consultarDashboardRetornaLista() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get(BASE_FORM + "/api/v1/questionnaires")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(5)
    @Disabled("E2E - requiere servicios corriendo en dev")
    void tokenInvalidoRetorna401() {
        given()
                .header("Authorization", "Bearer tokenFalso.abc.def")
                .when()
                .get(BASE_FORM + "/api/v1/questionnaires")
                .then()
                .statusCode(401);
    }
}