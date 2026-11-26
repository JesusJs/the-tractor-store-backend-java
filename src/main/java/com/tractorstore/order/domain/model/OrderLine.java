package com.tractorstore.order.domain.model;

/**
 * Line of an order with a historic copy of the catalog data. {@code variantName} is the catalog
 * name of the variant; the receipt shows the SKU instead (HTTP mapper, FR-009).
 */
public record OrderLine(
    String sku,
    String productId,
    String productName,
    String variantName,
    Money unitPrice,
    int quantity,
    String image) {

  private static final int MAX_QUANTITY = 99;

  public OrderLine {
    requireText(sku, "sku");
    requireText(productId, "productId");
    requireText(productName, "productName");
    requireText(variantName, "variantName");
    requireText(image, "image");
    if (unitPrice == null || !unitPrice.isPositive()) {
      throw new IllegalArgumentException("unitPrice must be positive: " + unitPrice);
    }
    if (quantity < 1 || quantity > MAX_QUANTITY) {
      throw new IllegalArgumentException("quantity must be between 1 and 99: " + quantity);
    }
  }

  public Money lineTotal() {
    return unitPrice.times(quantity);
  }

  private static void requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }
}
