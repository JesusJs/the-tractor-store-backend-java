package com.tractorstore.shared.infrastructure.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Anonymous browser session in the {@code ts_session} cookie (ADR 0011, research R2). The value is
 * a random UUID issued by the backend; anything else sent by the client is ignored and never
 * trusted.
 */
@Component
@EnableConfigurationProperties(SessionCookieProperties.class)
public class SessionCookies {

  public static final String COOKIE_NAME = "ts_session";

  /** {@code UUID.fromString} alone also accepts non canonical forms such as {@code 1-1-1-1-1}. */
  private static final Pattern CANONICAL_UUID =
      Pattern.compile("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$");

  private final SessionCookieProperties properties;

  public SessionCookies(SessionCookieProperties properties) {
    this.properties = properties;
  }

  /** Session of the first {@code ts_session} cookie, if it is a canonical UUID. */
  public Optional<UUID> read(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
        .findFirst()
        .map(Cookie::getValue)
        .filter(value -> CANONICAL_UUID.matcher(value).matches())
        .map(UUID::fromString);
  }

  /**
   * Valid session of the request; otherwise a new one, sent back with {@code Set-Cookie} (browser
   * session cookie: no {@code Max-Age}).
   */
  public UUID resolveOrIssue(HttpServletRequest request, HttpServletResponse response) {
    return read(request)
        .orElseGet(
            () -> {
              UUID session = UUID.randomUUID();
              ResponseCookie cookie =
                  ResponseCookie.from(COOKIE_NAME, session.toString())
                      .path("/")
                      .httpOnly(true)
                      .secure(properties.secureCookie())
                      .sameSite("Lax")
                      .build();
              response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
              return session;
            });
  }
}
