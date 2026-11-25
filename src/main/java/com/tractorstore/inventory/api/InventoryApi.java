package com.tractorstore.inventory.api;

/** What other modules may ask the inventory (contracts/module-apis.md). */
public interface InventoryApi {

  /**
   * Atomically decreases the stock of {@code sku} by {@code quantity} only if enough units are left
   * (single conditional UPDATE). Runs in the caller's transaction, so a later failure there rolls
   * it back.
   *
   * @return true if decreased; false if not enough stock or the SKU has no stock row
   * @throws IllegalArgumentException if quantity < 1
   */
  boolean decreaseStock(String sku, int quantity);
}
