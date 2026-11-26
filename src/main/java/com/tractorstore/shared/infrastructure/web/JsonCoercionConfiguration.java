package com.tractorstore.shared.infrastructure.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * A JSON number never becomes a {@code String} field ({@code "variantId": 12} is a 400
 * MALFORMED_REQUEST, FR-017). The opposite direction and float to int are switched off in {@code
 * application.yml} ({@code spring.jackson.*}); Jackson 3 has no property for this one.
 */
@Configuration(proxyBeanMethods = false)
public class JsonCoercionConfiguration {

  @Bean
  JsonMapperBuilderCustomizer rejectScalarsAsText() {
    return builder ->
        builder.withCoercionConfig(
            LogicalType.Textual,
            config ->
                config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
  }
}
