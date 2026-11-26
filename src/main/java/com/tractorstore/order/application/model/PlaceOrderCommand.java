package com.tractorstore.order.application.model;

import java.util.List;
import java.util.UUID;

/**
 * What the buyer asks for. Only SKU and quantity per line: prices and names come from the catalog.
 * {@code lines} may be null or empty (CART_EMPTY).
 */
public record PlaceOrderCommand(
    UUID sessionId,
    String firstName,
    String lastName,
    String storeId,
    String extraPickups,
    List<Line> lines) {

  public record Line(String sku, int quantity) {}
}
