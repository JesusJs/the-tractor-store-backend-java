package com.tractorstore.cart.infrastructure.web.dto;

import java.math.BigDecimal;

/**
 * Cart line ({@code CartItem} in the front). Never sent, since the server cart is always empty; it
 * only documents the shape of {@code items} in OpenAPI.
 */
public record CartItemResponse(
    String productId,
    String variantId,
    String productName,
    String variantName,
    BigDecimal price,
    int quantity,
    String image) {}
