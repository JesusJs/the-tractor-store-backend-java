package com.tractorstore.order.domain.exception;

import com.tractorstore.shared.error.ConflictException;

/** An order without lines. */
public class CartEmptyException extends ConflictException {

  public CartEmptyException() {
    super("CART_EMPTY", "Cart is empty");
  }
}
