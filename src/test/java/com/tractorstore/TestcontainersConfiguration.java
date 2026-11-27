package com.tractorstore;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared PostgreSQL container for integration tests (same image as compose.yaml). Each test context
 * gets a fresh container; if it is ever made static or reusable, the ITs that commit must clean
 * {@code event_publication} too ({@code OrderTestData.restoreSeed}, review C10), because pending
 * publications are resubmitted on startup.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  /** Pinned patch version; keep in sync with compose.yaml. */
  static final String POSTGRES_IMAGE = "postgres:17.12";

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE));
  }
}
