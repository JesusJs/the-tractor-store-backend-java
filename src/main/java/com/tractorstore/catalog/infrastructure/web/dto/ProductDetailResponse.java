package com.tractorstore.catalog.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Product detail page ({@code ProductDetail} in the front). */
public record ProductDetailResponse(
    String id,
    String name,
    String brand,
    BigDecimal price,
    String image,
    String description,
    List<String> variants,
    List<String> highlights) {}
