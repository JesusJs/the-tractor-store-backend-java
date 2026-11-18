package com.tractorstore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class ApplicationIT {

  @Autowired private RestTestClient client;

  @Test
  void openApiDocumentHasNoPaths() {
    client
        .get()
        .uri("/v3/api-docs")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.paths")
        .isEmpty();
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
  void businessRoutesDoNotExistYet() {
    client.get().uri("/api/v1/catalog/products").exchange().expectStatus().isNotFound();
  }
}
