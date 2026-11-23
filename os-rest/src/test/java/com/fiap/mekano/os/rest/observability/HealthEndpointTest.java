package com.fiap.mekano.os.rest.observability;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HealthEndpointTest {

    @Test
    void livenessIsUp() {
        given().when().get("/q/health/live").then().statusCode(200).body("status", equalTo("UP"));
    }
}
