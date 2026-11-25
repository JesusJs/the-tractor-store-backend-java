package com.tractorstore.order.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** UTC clock for {@code placedAt}; the use case only depends on {@code java.time} (FR-030). */
@Configuration(proxyBeanMethods = false)
class OrderClockConfiguration {

  @Bean
  Clock orderClock() {
    return Clock.systemUTC();
  }
}
