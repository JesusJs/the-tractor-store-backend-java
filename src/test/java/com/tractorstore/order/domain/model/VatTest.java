package com.tractorstore.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 21 % VAT rounded to two decimals HALF_UP (research R10). */
class VatTest {

  @ParameterizedTest
  @CsvSource({"0.05, 0.01", "0.02, 0.00", "2.50, 0.53", "5000, 1050.00", "43065000, 9043650.00"})
  void taxIsTwentyOnePercentHalfUp(String subTotal, String expected) {
    Money tax = Vat.taxOn(new Money(new BigDecimal(subTotal)));

    assertThat(tax.amount()).isEqualTo(new BigDecimal(expected));
  }
}
