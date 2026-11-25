package com.tractorstore.inventory.domain.port.out;

import com.tractorstore.inventory.domain.model.StockItem;
import java.util.Optional;

/** Output port: stock per SKU. */
public interface StockRepository {

  Optional<StockItem> findBySku(String sku);

  /**
   * Single atomic conditional update: takes {@code quantity} units only if that many are left.
   *
   * @return true if one row was decreased; false if not enough stock or no row for the SKU
   */
  boolean decreaseIfAvailable(String sku, int quantity);
}
