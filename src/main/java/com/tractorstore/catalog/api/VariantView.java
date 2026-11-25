package com.tractorstore.catalog.api;

import java.math.BigDecimal;

/** Catalog data of a purchasable variant, as other modules see it. */
public record VariantView(
    String sku,
    String productId,
    String productName,
    String variantName,
    BigDecimal price,
    String image) {}
