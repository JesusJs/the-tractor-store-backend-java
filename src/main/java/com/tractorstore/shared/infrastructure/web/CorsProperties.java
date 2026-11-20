package com.tractorstore.shared.infrastructure.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origins allowed to call {@code /api/**} with credentials (FR-027). No origin means none allowed;
 * any {@code *} makes startup fail, because a wildcard plus credentials would expose the session.
 */
@ConfigurationProperties("tractorstore.cors")
public record CorsProperties(List<String> allowedOrigins) {

  public CorsProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    for (String origin : allowedOrigins) {
      if (origin.contains("*")) {
        throw new IllegalStateException(
            "tractorstore.cors.allowed-origins must list explicit origins, found: " + origin);
      }
    }
  }
}
