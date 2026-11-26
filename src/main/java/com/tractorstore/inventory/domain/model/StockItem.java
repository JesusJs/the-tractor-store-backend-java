package com.tractorstore.inventory.domain.model;

/** Units left of a variant, identified by its SKU (same SKU as the catalog, no FK). */
public record StockItem(String sku, int units) {

  private static final int MAX_SKU_LENGTH = 20;

  public StockItem {
    if (sku == null || sku.isBlank() || sku.length() > MAX_SKU_LENGTH) {
      throw new IllegalArgumentException(
          "sku must not be blank and have at most " + MAX_SKU_LENGTH + " characters: " + sku);
    }
    if (units < 0) {
      throw new IllegalArgumentException("units must not be negative: " + units);
    }
  }

  /** Whether {@code quantity} (at least one) units can be taken. */
  public boolean canServe(int quantity) {
    return quantity >= 1 && units >= quantity;
  }
}
