package com.tractorstore.order.domain.exception;

import com.tractorstore.shared.error.ConflictException;

/** Not enough units left for a line: the whole order is rolled back. */
public class OutOfStockException extends ConflictException {

  public OutOfStockException(String sku) {
    super("OUT_OF_STOCK", "Variant " + sku + " is out of stock");
  }
}
