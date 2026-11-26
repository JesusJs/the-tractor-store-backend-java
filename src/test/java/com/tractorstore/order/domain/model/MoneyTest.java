package com.tractorstore.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

  @ParameterizedTest
  @CsvSource({"5000, 5000.00", "0.125, 0.13", "0.124, 0.12", "2.5, 2.50", "0, 0.00"})
  void isNormalizedToScaleTwoHalfUp(String raw, String expected) {
    Money money = new Money(new BigDecimal(raw));

    assertThat(money.amount()).isEqualTo(new BigDecimal(expected));
    assertThat(money.amount().scale()).isEqualTo(2);
  }

  @Test
  void negativeAmountIsRejected() {
    assertThatThrownBy(() -> new Money(new BigDecimal("-0.01")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void nullAmountIsRejected() {
    assertThatThrownBy(() -> new Money(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void zeroIsZeroWithScaleTwo() {
    assertThat(Money.ZERO.amount()).isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  void plusAddsAmounts() {
    Money sum = new Money(new BigDecimal("5000")).plus(new Money(new BigDecimal("1050.00")));

    assertThat(sum).isEqualTo(new Money(new BigDecimal("6050.00")));
  }

  @Test
  void timesMultipliesByAQuantity() {
    assertThat(new Money(new BigDecimal("8700.00")).times(99))
        .isEqualTo(new Money(new BigDecimal("861300.00")));
  }

  @Test
  void equalAmountsWithDifferentScaleAreEqual() {
    assertThat(new Money(new BigDecimal("5000"))).isEqualTo(new Money(new BigDecimal("5000.00")));
  }
}
