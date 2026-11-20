package com.tractorstore.shared.infrastructure.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS for the Angular micro-frontends; a disallowed origin gets Spring's 403 (research R10). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
class CorsConfiguration implements WebMvcConfigurer {

  private final CorsProperties properties;

  CorsConfiguration(CorsProperties properties) {
    this.properties = properties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
        .allowCredentials(true)
        .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
        .allowedHeaders("Content-Type", TraceIdFilter.HEADER)
        .exposedHeaders(TraceIdFilter.HEADER, "Location")
        .maxAge(3600);
  }
}
