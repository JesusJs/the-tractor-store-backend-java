package com.tractorstore.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CorsPropertiesTest {

  @ParameterizedTest
  @ValueSource(strings = {"*", "http://*.example", "https://localhost:*"})
  void wildcardOriginFailsFast(String origin) {
    assertThatThrownBy(() -> new CorsProperties(List.of("http://localhost:4200", origin)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("*");
  }

  @Test
  void missingOriginsMeanNoOriginAllowed() {
    assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
  }

  @Test
  void explicitOriginsAreKept() {
    assertThat(new CorsProperties(List.of("http://localhost:4200")).allowedOrigins())
        .containsExactly("http://localhost:4200");
  }
}
