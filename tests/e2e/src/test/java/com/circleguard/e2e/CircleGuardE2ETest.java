package com.circleguard.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

@Tag("e2e")
public class CircleGuardE2ETest {

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost:8080";
    }

    private String login() {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"testuser\",\"password\":\"password\"}")
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

    @Test
    public void testLoginComplete() {
        String token = login();
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/identities/me")
                .then()
                .statusCode(200);
    }

    @Test
    public void testCreateForm() {
        String token = login();
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"data\":\"test\"}")
                .when()
                .post("/forms")
                .then()
                .statusCode(201);
    }

    @Test
    public void testConsultForms() {
        String token = login();
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/forms")
                .then()
                .statusCode(200);
    }

    @Test
    public void testUploadFile() {
        String token = login();
        given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "test.txt", "hello".getBytes())
                .when()
                .post("/files/upload")
                .then()
                .statusCode(200);
    }

    @Test
    public void testInvalidToken() {
        given()
                .when()
                .get("/forms")
                .then()
                .statusCode(401);
    }
}
