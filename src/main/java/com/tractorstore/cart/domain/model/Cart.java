package com.tractorstore.cart.domain.model;

import java.math.BigDecimal;

/**
 * Server view of the cart. The cart lives in the browser (ADR 0011), so the server one is always
 * empty: there are no lines.
 */
public record Cart(int totalItems, BigDecimal subTotal, BigDecimal tax, BigDecimal total) {

  public static Cart empty() {
    return new Cart(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
  }
}
