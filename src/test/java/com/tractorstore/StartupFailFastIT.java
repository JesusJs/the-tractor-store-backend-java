package com.tractorstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Without a reachable database the application context must not start (fail-fast). No
 * Testcontainers: it points the datasource at a free port where nothing listens.
 */
class StartupFailFastIT {

  @Test
  void applicationDoesNotStartWithoutDatabase() throws IOException {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }

    // Command-line arguments: default properties would lose against ${DB_URL} in application.yml.
    String[] args = {
      "--spring.datasource.url=jdbc:postgresql://localhost:" + closedPort + "/tractor",
      "--spring.datasource.username=nobody",
      "--spring.datasource.password=nothing",
      "--spring.datasource.hikari.connection-timeout=2000",
      "--server.port=0"
    };
    SpringApplicationBuilder builder = new SpringApplicationBuilder(TractorStoreApplication.class);

    assertThatThrownBy(() -> builder.run(args))
        .satisfies(
            error ->
                assertThat(Stream.iterate(error, cause -> cause != null, Throwable::getCause))
                    .anySatisfy(cause -> assertThat(cause).isInstanceOf(ConnectException.class)));
  }
}
