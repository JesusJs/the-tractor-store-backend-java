package com.tractorstore.cart.infrastructure.web.controller;

import com.tractorstore.cart.application.port.in.GetCartUseCase;
import com.tractorstore.cart.domain.model.Cart;
import com.tractorstore.cart.infrastructure.web.dto.CartResponse;
import com.tractorstore.shared.infrastructure.web.ApiError;
import com.tractorstore.shared.infrastructure.web.SessionCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cart of the anonymous session. The front calls it only to obtain the {@code ts_session}. */
@RestController
@RequestMapping(CartPaths.BASE)
@Tag(name = "Cart", description = "Anonymous session cart (always empty on the server)")
class CartController {

  private final GetCartUseCase getCart;
  private final SessionCookies sessionCookies;

  CartController(GetCartUseCase getCart, SessionCookies sessionCookies) {
    this.getCart = getCart;
    this.sessionCookies = sessionCookies;
  }

  @GetMapping
  @Operation(
      summary = "Session cart",
      description =
          "Returns the empty cart (the cart lives in the browser). Without a valid ts_session"
              + " cookie it issues one: Set-Cookie ts_session=<uuid>; Path=/; HttpOnly;"
              + " SameSite=Lax (Secure in prod), no Max-Age.")
  @ApiResponse(
      responseCode = "200",
      description = "Empty cart",
      headers =
          @Header(
              name = "Set-Cookie",
              description = "ts_session, only when the request had no valid one"))
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected error",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  CartResponse cart(HttpServletRequest request, HttpServletResponse response) {
    sessionCookies.resolveOrIssue(request, response);
    Cart cart = getCart.get();
    return new CartResponse(
        List.of(), cart.totalItems(), cart.subTotal(), cart.tax(), cart.total());
  }
}
