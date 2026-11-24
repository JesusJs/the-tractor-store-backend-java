package com.tractorstore.catalog.infrastructure.web.dto;

import java.util.List;

/** Listing of a category ({@code CategoryData} in the front). */
public record CategoryResponse(
    String category, List<ProductItemResponse> products, List<String> availableFilters) {}
