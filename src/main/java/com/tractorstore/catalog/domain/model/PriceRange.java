package com.tractorstore.catalog.domain.model;

/** Throwaway class for SC-002 validation: price range with inclusive bounds. */
public final class PriceRange {

  private final int min;
  private final int max;

  private PriceRange(int min, int max) {
    this.min = min;
    this.max = max;
  }

  public static PriceRange of(int min, int max) {
    if (min < 0) {
      throw new IllegalArgumentException("min must not be negative");
    }
    if (max < min) {
      throw new IllegalArgumentException("max must not be lower than min");
    }
    return new PriceRange(min, max);
  }

  public boolean contains(int price) {
    return price >= min && price <= max;
  }

  public int width() {
    return max - min;
  }

  public PriceRange widen(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must not be negative");
    }
    return new PriceRange(Math.max(0, min - amount), max + amount);
  }

  public boolean overlaps(PriceRange other) {
    return min <= other.max && other.min <= max;
  }
}
