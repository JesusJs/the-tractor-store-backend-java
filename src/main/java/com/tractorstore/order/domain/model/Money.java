package com.tractorstore.order.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Non negative amount in euros, always with two decimals (HALF_UP when rounding, research R10). */
public record Money(BigDecimal amount) {

  public static final Money ZERO = new Money(BigDecimal.ZERO);

  private static final int SCALE = 2;

  public Money {
    if (amount == null) {
      throw new IllegalArgumentException("amount must not be null");
    }
    if (amount.signum() < 0) {
      throw new IllegalArgumentException("amount must not be negative: " + amount);
    }
    amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
  }

  public Money plus(Money other) {
    return new Money(amount.add(other.amount));
  }

  public Money times(int quantity) {
    return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
  }

  public boolean isPositive() {
    return amount.signum() > 0;
  }
}
