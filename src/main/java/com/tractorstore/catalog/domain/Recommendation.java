package com.tractorstore.catalog.domain;

/** Recommended product and its variant closest to the requested colors. */
public record Recommendation(Product product, Variant variant) {}
