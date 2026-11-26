package com.tractorstore.cart.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tractorstore.cart.domain.model.Cart;
import org.junit.jupiter.api.Test;

class GetCartServiceTest {

  @Test
  void theServerCartIsAlwaysEmpty() {
    assertThat(new GetCartService().get()).isEqualTo(Cart.empty());
  }
}
