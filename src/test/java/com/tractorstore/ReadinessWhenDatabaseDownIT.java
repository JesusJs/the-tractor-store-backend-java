package com.tractorstore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Stops its own database container after startup: readiness must go DOWN while liveness stays UP.
 * Uses a dedicated container so the shared one from {@link TestcontainersConfiguration} is never
 * stopped.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Testcontainers
class ReadinessWhenDatabaseDownIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(DockerImageName.parse(TestcontainersConfiguration.POSTGRES_IMAGE));

  @Autowired private RestTestClient client;

  @Test
  void readinessIsDownAndLivenessIsUpWhenDatabaseStops() {
    postgres.stop();

    client
        .get()
        .uri("/actuator/health/readiness")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectBody()
        .json("{\"status\":\"DOWN\"}", JsonCompareMode.STRICT);

    client
        .get()
        .uri("/actuator/health/liveness")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json("{\"status\":\"UP\"}", JsonCompareMode.STRICT);

    client
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isEqualTo(503)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("DOWN")
        .jsonPath("$.components")
        .doesNotExist()
        .jsonPath("$.details")
        .doesNotExist();
  }
}
