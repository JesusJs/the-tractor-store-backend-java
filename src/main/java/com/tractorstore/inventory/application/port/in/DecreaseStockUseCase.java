package com.tractorstore.inventory.application.port.in;

/** Inbound port: take units of a variant if they are left. */
public interface DecreaseStockUseCase {

  /**
   * @return true if decreased; false if not enough stock or unknown SKU
   * @throws IllegalArgumentException if quantity < 1
   */
  boolean decrease(String sku, int quantity);
}
