package com.tractorstore.catalog.domain.model;

/** Recommended product and its variant closest to the requested colors. */
public record Recommendation(Product product, Variant variant) {}
