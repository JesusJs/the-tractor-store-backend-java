package com.tractorstore.cart.application.service;

import com.tractorstore.cart.application.port.in.GetCartUseCase;
import com.tractorstore.cart.domain.model.Cart;
import org.springframework.stereotype.Service;

/** No transaction: it does not touch the database. */
@Service
class GetCartService implements GetCartUseCase {

  @Override
  public Cart get() {
    return Cart.empty();
  }
}
