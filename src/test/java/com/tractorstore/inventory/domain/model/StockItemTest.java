package com.tractorstore.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StockItemTest {

  @Test
  void keepsSkuAndUnits() {
    StockItem item = new StockItem("CL-01-GR", 10);

    assertThat(item.sku()).isEqualTo("CL-01-GR");
    assertThat(item.units()).isEqualTo(10);
  }

  @Test
  void zeroUnitsIsAllowed() {
    assertThat(new StockItem("CL-01-GY", 0).units()).isZero();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  ", "ABCDEFGHIJKLMNOPQRSTU"})
  void skuMustBeNonBlankAndAtMost20Characters(String sku) {
    assertThatThrownBy(() -> new StockItem(sku, 1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void skuOf20CharactersIsAllowed() {
    assertThat(new StockItem("ABCDEFGHIJKLMNOPQRST", 1).sku()).hasSize(20);
  }

  @Test
  void negativeUnitsAreRejected() {
    assertThatThrownBy(() -> new StockItem("CL-01-GR", -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({"5, 0, false", "5, -1, false", "5, 4, true", "5, 5, true", "5, 6, false"})
  void canServeOnlyPositiveQuantitiesUpToTheUnits(int units, int quantity, boolean expected) {
    assertThat(new StockItem("AU-07-YE", units).canServe(quantity)).isEqualTo(expected);
  }
}
