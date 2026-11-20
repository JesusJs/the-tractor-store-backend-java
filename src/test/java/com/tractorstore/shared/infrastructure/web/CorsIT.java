package com.tractorstore.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tractorstore.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Uses the real dev defaults of application-dev.yml; the container replaces the dev database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles({"test", "dev"})
class CorsIT {

  private static final String HOME = "/api/v1/catalog/home";

  @Autowired private RestTestClient client;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://localhost:4200",
        "http://localhost:4201",
        "http://localhost:4202",
        "http://localhost:4203"
      })
  void allowedOriginGetsCorsHeadersOnSimpleRequest(String origin) {
    client
        .get()
        .uri(HOME)
        .header(HttpHeaders.ORIGIN, origin)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
        .expectHeader()
        .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
        .expectHeader()
        .value(
            HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
            exposed -> assertThat(exposed).contains(TraceIdFilter.HEADER));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://localhost:4200",
        "http://localhost:4201",
        "http://localhost:4202",
        "http://localhost:4203"
      })
  void allowedOriginPassesPreflight(String origin) {
    client
        .method(HttpMethod.OPTIONS)
        .uri(HOME)
        .header(HttpHeaders.ORIGIN, origin)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
        .expectHeader()
        .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
  }

  @Test
  void unknownOriginIsRejected() {
    client
        .get()
        .uri(HOME)
        .header(HttpHeaders.ORIGIN, "http://evil.example")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectHeader()
        .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
  }

  @Test
  void requestWithoutOriginHasNoCorsHeaders() {
    client
        .get()
        .uri(HOME)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
        .expectHeader()
        .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
  }
}
