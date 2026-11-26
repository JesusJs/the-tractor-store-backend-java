package com.tractorstore.order.domain.exception;

import com.tractorstore.shared.error.NotFoundException;

/**
 * Unknown order, or an order of another session. Fixed message without the id, so the response
 * never echoes the input nor tells both cases apart.
 */
public class OrderNotFoundException extends NotFoundException {

  public OrderNotFoundException() {
    super("ORDER_NOT_FOUND", "Order not found");
  }
}
