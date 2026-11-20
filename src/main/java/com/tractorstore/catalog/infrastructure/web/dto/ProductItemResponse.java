package com.tractorstore.catalog.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Product of a listing or a recommendation ({@code Tractor} in the front). */
public record ProductItemResponse(
    String id,
    String name,
    String brand,
    BigDecimal price,
    String image,
    List<String> variants,
    String description) {}
