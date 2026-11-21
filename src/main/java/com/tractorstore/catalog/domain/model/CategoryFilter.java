package com.tractorstore.catalog.domain.model;

import java.util.Optional;

/**
 * Listing filter from the URL: {@code all} or a category code to resolve. Comparison is exact and
 * case sensitive ({@code Classic} is not {@code classic}).
 */
public final class CategoryFilter {

  public static final String ALL = "all";

  private final String value;

  private CategoryFilter(String value) {
    this.value = value;
  }

  public static CategoryFilter of(String raw) {
    if (raw == null || raw.isEmpty()) {
      throw new IllegalArgumentException("filter must not be empty");
    }
    return new CategoryFilter(raw);
  }

  public boolean isAll() {
    return ALL.equals(value);
  }

  /** Category code to resolve; empty for {@code all}. */
  public Optional<String> categoryCode() {
    return isAll() ? Optional.empty() : Optional.of(value);
  }

  public String value() {
    return value;
  }
}
