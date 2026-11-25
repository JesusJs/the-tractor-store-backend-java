package com.tractorstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.tractorstore.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/** End to end: stock of a variant over HTTP against the V4 seed (AS-1.1..1.4). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class InventoryApiIT {

  private static final String BASE = "/api/v1/inventory";
  private static final String TRACE = "X-Trace-Id";

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setStock() {
    // Own values: other ITs may change the shared stock (research R15).
    jdbc.update("update inventory_stock set stock = 10 where sku = 'CL-01-GR'");
    jdbc.update("update inventory_stock set stock = 0 where sku = 'CL-01-GY'");
  }

  @Test
  void variantInStock() {
    client
        .get()
        .uri(BASE + "/CL-01-GR")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .doesNotExist(HttpHeaders.SET_COOKIE)
        .expectBody()
        .json(
            """
            { "sku": "CL-01-GR", "stock": 10 }
            """);
  }

  @Test
  void variantOutOfStockIsStillOk() {
    client
        .get()
        .uri(BASE + "/CL-01-GY")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.stock")
        .isEqualTo(0);
  }

  @Test
  void unknownSkuIsVariantNotFound() {
    EntityExchangeResult<String> result =
        client
            .get()
            .uri(BASE + "/XX-99-ZZ")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .doesNotExist(HttpHeaders.SET_COOKIE)
            .expectBody(String.class)
            .returnResult();

    String body = result.getResponseBody();
    assertThat(JsonPath.<String>read(body, "$.code")).isEqualTo("VARIANT_NOT_FOUND");
    assertThat(JsonPath.<String>read(body, "$.message")).isEqualTo("Variant XX-99-ZZ not found");
    assertThat(JsonPath.<String>read(body, "$.traceId"))
        .isEqualTo(result.getResponseHeaders().getFirst(TRACE));
  }

  @Test
  void skuLongerThan20IsValidationError() {
    client
        .get()
        .uri(BASE + "/ABCDEFGHIJKLMNOPQRSTU")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("VALIDATION_ERROR")
        .jsonPath("$.details.sku")
        .exists()
        .jsonPath("$.traceId")
        .exists();
  }
}
