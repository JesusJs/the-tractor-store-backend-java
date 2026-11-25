package com.tractorstore.cart.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tractorstore.cart.application.port.in.GetCartUseCase;
import com.tractorstore.cart.domain.model.Cart;
import com.tractorstore.shared.infrastructure.web.GlobalExceptionHandler;
import com.tractorstore.shared.infrastructure.web.SessionCookies;
import com.tractorstore.shared.infrastructure.web.TraceIdFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** {@code SessionCookies} registers {@code SessionCookieProperties} (secure-cookie false). */
@WebMvcTest(CartController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class, SessionCookies.class})
class CartControllerTest {

  private static final String CART = "/api/v1/cart";
  private static final String VALID = "3f0c2a4e-8b1d-4c7a-9e5f-1a2b3c4d5e6f";

  @Autowired private MockMvc mvc;

  @MockitoBean private GetCartUseCase getCart;

  @BeforeEach
  void emptyCart() {
    when(getCart.get()).thenReturn(Cart.empty());
  }

  @Test
  void returnsTheEmptyCartWithTheFrontShape() throws Exception {
    mvc.perform(get(CART))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(0))
        .andExpect(jsonPath("$.totalItems").value(0))
        .andExpect(jsonPath("$.subTotal").value(0))
        .andExpect(jsonPath("$.tax").value(0))
        .andExpect(jsonPath("$.total").value(0));
  }

  @Test
  void withoutCookieIssuesTheSessionCookie() throws Exception {
    String setCookie =
        mvc.perform(get(CART))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.SET_COOKIE);

    assertThat(setCookie)
        .matches("ts_session=[0-9a-f-]{36}; Path=/; HttpOnly; SameSite=Lax")
        .doesNotContain("Secure");
  }

  @Test
  void validCookieIsKeptWithoutSetCookie() throws Exception {
    mvc.perform(get(CART).cookie(new Cookie("ts_session", VALID)))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  @Test
  void invalidCookieIsReplacedByANewOne() throws Exception {
    String setCookie =
        mvc.perform(get(CART).cookie(new Cookie("ts_session", "no-uuid")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.SET_COOKIE);

    assertThat(setCookie).startsWith("ts_session=").doesNotContain("no-uuid");
  }
}
