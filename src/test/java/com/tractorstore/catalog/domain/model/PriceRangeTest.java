package com.tractorstore.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PriceRangeTest {

  @Test
  void containsIsInclusive() {
    PriceRange range = PriceRange.of(10, 20);

    assertThat(range.contains(10)).isTrue();
    assertThat(range.contains(20)).isTrue();
    assertThat(range.contains(9)).isFalse();
    assertThat(range.contains(21)).isFalse();
    assertThat(range.width()).isEqualTo(10);
  }

  @Test
  void invalidBoundsAreRejected() {
    assertThatThrownBy(() -> PriceRange.of(-1, 5)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PriceRange.of(5, 4)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void widenNeverGoesBelowZero() {
    PriceRange widened = PriceRange.of(3, 10).widen(5);

    assertThat(widened.contains(0)).isTrue();
    assertThat(widened.contains(15)).isTrue();
    assertThat(widened.width()).isEqualTo(15);
    assertThatThrownBy(() -> widened.widen(-1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void overlapsDetectsSharedPrices() {
    PriceRange range = PriceRange.of(10, 20);

    assertThat(range.overlaps(PriceRange.of(20, 30))).isTrue();
    assertThat(range.overlaps(PriceRange.of(0, 9))).isFalse();
    assertThat(range.overlaps(PriceRange.of(21, 30))).isFalse();
  }
}
