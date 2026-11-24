package com.tractorstore.shared.infrastructure.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origins allowed to call {@code /api/**} with credentials (FR-027). No origin means none allowed.
 * Values are trimmed; any {@code *}, blank value or the {@code null} origin makes startup fail,
 * because with credentials they would expose the session (review 002 #7).
 */
@ConfigurationProperties("tractorstore.cors")
public record CorsProperties(List<String> allowedOrigins) {

  public CorsProperties {
    allowedOrigins =
        allowedOrigins == null
            ? List.of()
            : allowedOrigins.stream().map(CorsProperties::checked).toList();
  }

  private static String checked(String origin) {
    String trimmed = origin == null ? "" : origin.strip();
    if (trimmed.isEmpty() || trimmed.contains("*") || trimmed.equalsIgnoreCase("null")) {
      throw new IllegalStateException(
          "tractorstore.cors.allowed-origins must list explicit origins, found: '" + origin + "'");
    }
    return trimmed;
  }
}
