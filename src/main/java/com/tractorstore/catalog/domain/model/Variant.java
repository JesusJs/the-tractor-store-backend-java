package com.tractorstore.catalog.domain.model;

import static com.tractorstore.catalog.domain.model.DomainChecks.requireText;
import static com.tractorstore.catalog.domain.model.DomainChecks.requireValue;

import java.math.BigDecimal;

/** Purchasable variant of a product, identified by its SKU. */
public record Variant(String sku, String name, Color color, BigDecimal price, String image) {

  public Variant {
    requireText(sku, "sku");
    requireText(name, "name");
    requireValue(color, "color");
    requireValue(price, "price");
    requireText(image, "image");
    if (price.signum() <= 0) {
      throw new IllegalArgumentException("price must be positive: " + price);
    }
  }
}
