package com.tractorstore.cart.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Cart with the front shape ({@code Cart} in the front). */
public record CartResponse(
    List<CartItemResponse> items,
    int totalItems,
    BigDecimal subTotal,
    BigDecimal tax,
    BigDecimal total) {}
