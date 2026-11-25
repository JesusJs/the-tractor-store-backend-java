package com.tractorstore.shared.infrastructure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Attributes of the {@code ts_session} cookie (ADR 0011). {@code secureCookie} adds {@code Secure};
 * only the prod profile, served over HTTPS, turns it on.
 */
@ConfigurationProperties("tractorstore.session")
public record SessionCookieProperties(boolean secureCookie) {}
