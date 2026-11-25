package com.tractorstore.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.tractorstore.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/** End to end: the real {@code ts_session} cookie of {@code GET /cart} (AS-1.5..1.7). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class CartApiIT {

  private static final String CART = "/api/v1/cart";
  private static final String COOKIE =
      "ts_session=([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12});"
          + " Path=/; HttpOnly; SameSite=Lax";

  @Autowired private RestTestClient client;

  @Test
  void withoutCookieReturnsTheEmptyCartAndIssuesTheSession() {
    List<String> setCookie =
        client
            .get()
            .uri(CART)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(
                """
                { "items": [], "totalItems": 0, "subTotal": 0, "tax": 0, "total": 0 }
                """)
            .returnResult()
            .getResponseHeaders()
            .get(HttpHeaders.SET_COOKIE);

    // test profile: no Secure (only prod turns it on)
    assertThat(setCookie).singleElement().asString().matches(COOKIE).doesNotContain("Secure");
  }

  @Test
  void issuedCookieIsKeptOnTheNextRequest() {
    String session = issueSession();

    client
        .get()
        .uri(CART)
        .cookie("ts_session", session)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .doesNotExist(HttpHeaders.SET_COOKIE);
  }

  @Test
  void invalidCookieIsReplaced() {
    String session =
        client
            .get()
            .uri(CART)
            .cookie("ts_session", "1-1-1-1-1")
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult()
            .getResponseHeaders()
            .getFirst(HttpHeaders.SET_COOKIE);

    assertThat(session).matches(COOKIE).doesNotContain("1-1-1-1-1");
  }

  private String issueSession() {
    String setCookie =
        client
            .get()
            .uri(CART)
            .exchange()
            .returnResult()
            .getResponseHeaders()
            .getFirst(HttpHeaders.SET_COOKIE);
    assertThat(setCookie).matches(COOKIE);
    return setCookie.substring("ts_session=".length(), setCookie.indexOf(';'));
  }
}
