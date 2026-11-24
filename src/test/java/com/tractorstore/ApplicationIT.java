package com.tractorstore;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class ApplicationIT {

  @Autowired private RestTestClient client;

  private static final String CATALOG = "/api/v1/catalog";

  static String[] catalogPaths() {
    return new String[] {
      CATALOG + "/home",
      CATALOG + "/categories/{filter}",
      CATALOG + "/products/{id}",
      CATALOG + "/recommendations",
      CATALOG + "/stores"
    };
  }

  @Test
  void openApiDocumentHasExactlyTheFiveCatalogPaths() {
    Map<String, Object> paths = apiDocs().read("$.paths");

    assertThat(paths).containsOnlyKeys(catalogPaths());
  }

  @ParameterizedTest
  @MethodSource("catalogPaths")
  void everyCatalogOperationIsDocumented(String path) {
    DocumentContext docs = apiDocs();
    String operation = "$.paths['" + path + "'].get";

    assertThat(docs.read(operation + ".tags", String[].class)).containsExactly("Catalog");
    assertThat(docs.read(operation + ".summary", String.class)).isNotBlank();
    assertThat(docs.read(operation + ".description", String.class)).isNotBlank();
    Map<String, Object> responses = docs.read(operation + ".responses");
    assertThat(responses).containsKeys("200", "500");
    responses.keySet().stream()
        .filter(status -> !status.equals("200"))
        .forEach(
            status ->
                assertThat(
                        docs.read(
                            operation + ".responses['" + status + "'].content.*.schema['$ref']",
                            String[].class))
                    .as("%s %s error schema", path, status)
                    .containsOnly("#/components/schemas/ApiError"));
  }

  @Test
  void unknownCatalogRouteReturnsNotFoundError() {
    client
        .get()
        .uri(CATALOG + "/products")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .exists("X-Trace-Id")
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("NOT_FOUND")
        .jsonPath("$.traceId")
        .isNotEmpty();
  }

  private DocumentContext apiDocs() {
    String body =
        client
            .get()
            .uri("/v3/api-docs")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    return JsonPath.parse(body);
  }

  @Test
  void swaggerUiIsServed() {
    client
        .get()
        .uri("/swagger-ui/index.html")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.TEXT_HTML);
  }

  @Test
  void swaggerUiShortcutRedirectsToIndex() {
    client
        .get()
        .uri("/swagger-ui.html")
        .exchange()
        .expectStatus()
        .isFound()
        .expectHeader()
        .value("Location", location -> location.endsWith("/swagger-ui/index.html"));
  }

  @Test
  void healthReportsOnlyGlobalStatus() {
    // Boot always lists group names at the root; they are not components or details (FR-009).
    client
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP")
        .jsonPath("$.components")
        .doesNotExist()
        .jsonPath("$.details")
        .doesNotExist();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/actuator/health/liveness", "/actuator/health/readiness"})
  void probesReportOnlyGlobalStatus(String path) {
    client
        .get()
        .uri(path)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json("{\"status\":\"UP\"}", JsonCompareMode.STRICT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/actuator/env",
        "/actuator/configprops",
        "/actuator/beans",
        "/actuator/mappings",
        "/actuator/info",
        "/actuator/metrics",
        "/actuator/modulith"
      })
  void otherManagementEndpointsAreNotExposed(String path) {
    client.get().uri(path).exchange().expectStatus().isNotFound();
  }
}
