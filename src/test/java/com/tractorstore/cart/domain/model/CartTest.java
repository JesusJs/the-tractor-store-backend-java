package com.tractorstore.cart.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CartTest {

  @Test
  void emptyCartHasNoItemsAndZeroAmounts() {
    Cart cart = Cart.empty();

    assertThat(cart.totalItems()).isZero();
    assertThat(cart.subTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(cart.tax()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(cart.total()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
