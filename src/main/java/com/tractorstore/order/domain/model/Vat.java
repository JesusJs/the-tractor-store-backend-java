package com.tractorstore.order.domain.model;

import java.math.BigDecimal;

/** Spanish VAT applied to every order: 21 %, two decimals HALF_UP (research R10). */
public final class Vat {

  public static final BigDecimal RATE = new BigDecimal("0.21");

  private Vat() {}

  public static Money taxOn(Money subTotal) {
    return new Money(subTotal.amount().multiply(RATE));
  }
}
