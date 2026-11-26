package com.tractorstore.cart.application.port.in;

import com.tractorstore.cart.domain.model.Cart;

/** Inbound port: cart of the current session. */
public interface GetCartUseCase {

  /** Always empty: there is no server side cart (ADR 0011). */
  Cart get();
}
