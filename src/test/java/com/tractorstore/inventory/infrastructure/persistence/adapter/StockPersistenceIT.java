package com.tractorstore.inventory.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tractorstore.TestcontainersConfiguration;
import com.tractorstore.inventory.domain.model.StockItem;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Stock seeded by V4 and the conditional UPDATE, on the real migrations (FR-003, FR-004). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, StockRepositoryAdapter.class})
@ActiveProfiles("test")
class StockPersistenceIT {

  @Autowired private JdbcTemplate jdbc;
  @Autowired private StockRepositoryAdapter stock;

  @Test
  void seedHasOneRowPerCatalogVariant() {
    // Only the test crosses module tables; production code never does (constitution IV).
    List<String> stockSkus =
        jdbc.queryForList("select sku from inventory_stock order by sku", String.class);
    List<String> variantSkus =
        jdbc.queryForList("select sku from catalog_variant order by sku", String.class);

    assertThat(stockSkus).hasSize(47).isEqualTo(variantSkus);
  }

  @Test
  void seedHasTheDemoValues() {
    Map<String, Integer> special = Map.of("CL-01-GY", 0, "AU-08-WH", 1, "AU-07-YE", 5);

    special.forEach((sku, units) -> assertThat(units(sku)).as(sku).isEqualTo(units));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from inventory_stock where sku not in ('CL-01-GY', 'AU-08-WH',"
                    + " 'AU-07-YE') and stock <> 10",
                Integer.class))
        .isZero();
  }

  @Test
  void stockCanNeverBeNegative() {
    Integer checks =
        jdbc.queryForObject(
            "select count(*) from pg_constraint where conrelid = 'inventory_stock'::regclass"
                + " and contype = 'c' and pg_get_constraintdef(oid) like '%stock >= 0%'",
            Integer.class);

    assertThat(checks).isEqualTo(1);
  }

  @Test
  void findsTheStockOfAKnownSku() {
    setStock("CL-01-GR", 10);

    assertThat(stock.findBySku("CL-01-GR")).contains(new StockItem("CL-01-GR", 10));
  }

  @Test
  void unknownSkuIsEmpty() {
    assertThat(stock.findBySku("XX-99-ZZ")).isEmpty();
  }

  @Test
  void decreasesWhenThereIsEnoughStock() {
    setStock("AU-07-YE", 5);

    assertThat(stock.decreaseIfAvailable("AU-07-YE", 5)).isTrue();

    assertThat(units("AU-07-YE")).isZero();
  }

  @Test
  void doesNotDecreaseWhenThereIsNotEnoughStock() {
    setStock("AU-08-WH", 1);

    assertThat(stock.decreaseIfAvailable("AU-08-WH", 2)).isFalse();

    assertThat(units("AU-08-WH")).isEqualTo(1);
  }

  @Test
  void skuWithoutRowIsNotDecreased() {
    assertThat(stock.decreaseIfAvailable("XX-99-ZZ", 1)).isFalse();
  }

  /**
   * Own value for the decrease tests (research R15); rolled back with the test transaction, so the
   * seed tests still see V4. ITs that commit stock changes must restore the seed afterwards.
   */
  private void setStock(String sku, int units) {
    jdbc.update("update inventory_stock set stock = ? where sku = ?", units, sku);
  }

  private int units(String sku) {
    return jdbc.queryForObject(
        "select stock from inventory_stock where sku = ?", Integer.class, sku);
  }
}
