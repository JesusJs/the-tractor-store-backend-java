package com.tractorstore.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CategoryFilterTest {

  @Test
  void allMeansNoCategory() {
    CategoryFilter filter = CategoryFilter.of("all");

    assertThat(filter.isAll()).isTrue();
    assertThat(filter.categoryCode()).isEmpty();
    assertThat(filter.value()).isEqualTo("all");
  }

  @ParameterizedTest
  @ValueSource(strings = {"classic", "autonomous"})
  void categoryCodesAreKeptExactly(String raw) {
    CategoryFilter filter = CategoryFilter.of(raw);

    assertThat(filter.isAll()).isFalse();
    assertThat(filter.categoryCode()).contains(raw);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Classic", "classics", "ALL"})
  void comparisonIsExactAndCaseSensitive(String raw) {
    CategoryFilter filter = CategoryFilter.of(raw);

    assertThat(filter.isAll()).isFalse();
    assertThat(filter.categoryCode()).contains(raw);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void rejectsMissingValue(String raw) {
    assertThatThrownBy(() -> CategoryFilter.of(raw)).isInstanceOf(IllegalArgumentException.class);
  }
}
