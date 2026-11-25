package com.tractorstore.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** {@code ts_session} cookie without a Spring context (research R2). */
class SessionCookiesTest {

  private static final String VALID = "3f0c2a4e-8b1d-4c7a-9e5f-1a2b3c4d5e6f";

  private final SessionCookies cookies = new SessionCookies(new SessionCookieProperties(false));

  private static MockHttpServletRequest requestWith(Cookie... sent) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (sent.length > 0) {
      request.setCookies(sent);
    }
    return request;
  }

  @Nested
  class Read {

    @Test
    void withoutCookieIsEmpty() {
      assertThat(cookies.read(requestWith())).isEmpty();
    }

    @Test
    void canonicalUuidIsReturned() {
      assertThat(cookies.read(requestWith(new Cookie("ts_session", VALID))))
          .contains(UUID.fromString(VALID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1-1-1-1-1", "abc", "", "3f0c2a4e-8b1d-4c7a-9e5f-1a2b3c4d5e6f0"})
    void nonCanonicalValueIsEmpty(String value) {
      assertThat(cookies.read(requestWith(new Cookie("ts_session", value)))).isEmpty();
    }

    @Test
    void otherCookiesAreIgnored() {
      assertThat(cookies.read(requestWith(new Cookie("other", VALID)))).isEmpty();
    }

    @Test
    void firstSessionCookieWins() {
      String second = UUID.randomUUID().toString();

      assertThat(
              cookies.read(
                  requestWith(new Cookie("ts_session", VALID), new Cookie("ts_session", second))))
          .contains(UUID.fromString(VALID));
    }
  }

  @Nested
  class ResolveOrIssue {

    @Test
    void validCookieIsReusedWithoutSetCookie() {
      MockHttpServletResponse response = new MockHttpServletResponse();

      UUID session = cookies.resolveOrIssue(requestWith(new Cookie("ts_session", VALID)), response);

      assertThat(session).isEqualTo(UUID.fromString(VALID));
      assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    void withoutCookieIssuesANewSessionCookie() {
      MockHttpServletResponse response = new MockHttpServletResponse();

      UUID session = cookies.resolveOrIssue(requestWith(), response);

      assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
          .containsExactly("ts_session=" + session + "; Path=/; HttpOnly; SameSite=Lax");
    }

    @Test
    void invalidCookieIsReplacedByADifferentValue() {
      MockHttpServletResponse response = new MockHttpServletResponse();

      UUID session =
          cookies.resolveOrIssue(requestWith(new Cookie("ts_session", "1-1-1-1-1")), response);

      assertThat(session.toString()).isNotEqualTo("1-1-1-1-1").matches("[0-9a-f-]{36}");
      assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
          .startsWith("ts_session=" + session + ";")
          .doesNotContain("Max-Age")
          .doesNotContain("Expires")
          .doesNotContain("Secure");
    }

    @Test
    void eachIssuedSessionIsDifferent() {
      UUID first = cookies.resolveOrIssue(requestWith(), new MockHttpServletResponse());
      UUID second = cookies.resolveOrIssue(requestWith(), new MockHttpServletResponse());

      assertThat(first).isNotEqualTo(second);
    }

    @Test
    void secureCookieAddsSecure() {
      SessionCookies secure = new SessionCookies(new SessionCookieProperties(true));
      MockHttpServletResponse response = new MockHttpServletResponse();

      UUID session = secure.resolveOrIssue(requestWith(), response);

      assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
          .isEqualTo("ts_session=" + session + "; Path=/; Secure; HttpOnly; SameSite=Lax");
    }
  }
}
