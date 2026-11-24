package com.tractorstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.tractorstore.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/** End to end: the five catalog endpoints over HTTP against the Flyway dataset. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class CatalogApiIT {

  private static final String BASE = "/api/v1/catalog";
  private static final String TRACE = "X-Trace-Id";

  /** Example of contracts/catalog-api.md, compared strictly (no extra fields). */
  private static final String AU_07 =
      """
      {
        "id": "AU-07", "name": "Verde Voyager", "brand": "Autonomous", "price": 4000.00,
        "image": "https://blueprint.the-tractor.store/cdn/img/product/400/AU-07-MT.webp",
        "description": "Adaptive drive system intelligently navigates through diverse field conditions. Clean energy operation with advanced solar battery technology. High-resolution field scanners for precise agronomy insights",
        "variants": ["AU-07-MT", "AU-07-YE"],
        "highlights": [
          "Adaptive drive system intelligently navigates through diverse field conditions",
          "Clean energy operation with advanced solar battery technology",
          "High-resolution field scanners for precise agronomy insights"
        ]
      }
      """;

  @Autowired private RestTestClient client;

  @Test
  void homeHasTwoTeasers() {
    client
        .get()
        .uri(BASE + "/home")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists(TRACE)
        .expectBody()
        .jsonPath("$.length()")
        .isEqualTo(2)
        .jsonPath("$[0].filter")
        .isEqualTo("classic")
        .jsonPath("$[1].filter")
        .isEqualTo("autonomous");
  }

  @ParameterizedTest
  @CsvSource({"all, 23, CL-01", "classic, 15, CL-01", "autonomous, 8, AU-01"})
  void listingsHaveTheDatasetProducts(String filter, int count, String first) {
    client
        .get()
        .uri(BASE + "/categories/" + filter)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists(TRACE)
        .expectBody()
        .jsonPath("$.category")
        .isEqualTo(filter)
        .jsonPath("$.products.length()")
        .isEqualTo(count)
        .jsonPath("$.products[0].id")
        .isEqualTo(first)
        .jsonPath("$.availableFilters.length()")
        .isEqualTo(3);
  }

  @Test
  void detailMatchesTheContractExample() {
    client
        .get()
        .uri(BASE + "/products/AU-07")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists(TRACE)
        .expectBody()
        .json(AU_07, JsonCompareMode.STRICT);
  }

  @Test
  void recommendationsAreByColor() {
    client
        .get()
        .uri(BASE + "/recommendations?skus=AU-01-SI")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists(TRACE)
        .expectBody()
        .jsonPath("$[*].variants[0]")
        .isEqualTo(List.of("AU-07-MT", "CL-15-SD", "CL-08-GR", "CL-11-SK"));
  }

  @Test
  @DisplayName("regresión: sin skus responde 200 con la lista por defecto")
  void withoutSkusAlwaysReturnsTheDefaultList() {
    for (int i = 0; i < 100; i++) {
      client
          .get()
          .uri(BASE + "/recommendations")
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$[*].variants[0]")
          .isEqualTo(List.of("CL-01-GR", "CL-02-BL", "CL-03-GR", "CL-04-BL"));
    }
  }

  @Test
  void storesAreTheFourOfTheDataset() {
    client
        .get()
        .uri(BASE + "/stores")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists(TRACE)
        .expectBody()
        .jsonPath("$.length()")
        .isEqualTo(4)
        .jsonPath("$[0].address")
        .isEqualTo("Astronaut Way 1");
  }

  @ParameterizedTest
  @CsvSource({
    "/products/CL-99, 404, PRODUCT_NOT_FOUND",
    "/categories/classics, 404, CATEGORY_NOT_FOUND",
    "/recommendations?skus=AU-01-SI-XXXXXXXXXXXX, 400, VALIDATION_ERROR"
  })
  void errorsUseTheSingleShape(String path, int status, String code) {
    EntityExchangeResult<String> result =
        client
            .get()
            .uri(BASE + path)
            .exchange()
            .expectStatus()
            .isEqualTo(status)
            .expectBody(String.class)
            .returnResult();

    assertErrorWithTraceIdOfHeader(result, code);
  }

  @Test
  void postToHomeIsMethodNotAllowed() {
    EntityExchangeResult<String> result =
        client
            .post()
            .uri(BASE + "/home")
            .exchange()
            .expectStatus()
            .isEqualTo(405)
            .expectBody(String.class)
            .returnResult();

    assertErrorWithTraceIdOfHeader(result, "METHOD_NOT_ALLOWED");
  }

  @Test
  void unknownRouteIsNotFound() {
    EntityExchangeResult<String> result =
        client
            .get()
            .uri(BASE + "/products")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody(String.class)
            .returnResult();

    assertErrorWithTraceIdOfHeader(result, "NOT_FOUND");
  }

  /** SC-005 over real HTTP: the body {@code traceId} is the {@code X-Trace-Id} header. */
  private static void assertErrorWithTraceIdOfHeader(
      EntityExchangeResult<String> result, String code) {
    String header = result.getResponseHeaders().getFirst(TRACE);
    String body = result.getResponseBody();
    assertThat(header).isNotBlank();
    assertThat(JsonPath.<String>read(body, "$.code")).isEqualTo(code);
    assertThat(JsonPath.<String>read(body, "$.traceId")).isEqualTo(header);
  }

  @Test
  void validIncomingTraceIdIsReused() {
    client
        .get()
        .uri(BASE + "/products/CL-99")
        .header(TRACE, "demo-trace-1")
        .exchange()
        .expectHeader()
        .valueEquals(TRACE, "demo-trace-1")
        .expectBody()
        .jsonPath("$.traceId")
        .isEqualTo("demo-trace-1");
  }
}
