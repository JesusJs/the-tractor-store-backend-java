package com.tractorstore.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;

/**
 * Only the prod profile marks {@code ts_session} as {@code Secure} (constitution VI, analyze 004
 * G4, review PR #5 A03): a typo in either file would otherwise pass CI.
 */
class SessionCookieProfileTests {

  private static final Path RESOURCES = Path.of("src/main/resources");
  private static final String SECURE_COOKIE = "tractorstore.session.secure-cookie";

  @Test
  void baseConfigurationDoesNotRequireHttps() throws IOException {
    assertThat(secureCookie("application.yml")).isEqualTo(false);
  }

  @Test
  void prodProfileMarksTheCookieSecure() throws IOException {
    assertThat(secureCookie("application-prod.yml")).isEqualTo(true);
  }

  private static Object secureCookie(String fileName) throws IOException {
    Path file = RESOURCES.resolve(fileName);
    return new YamlPropertySourceLoader()
        .load(fileName, new FileSystemResource(file)).stream()
            .map(source -> source.getProperty(SECURE_COOKIE))
            .filter(value -> value != null)
            .findFirst()
            .orElse(null);
  }
}
