package com.tractorstore.order;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;

/** Shared state of the order ITs: they commit, so they must leave the migrated data as it was. */
final class OrderTestData {

  private static final Path STOCK_MIGRATION =
      Path.of("src/main/resources/db/migration/V4__inventory_stock.sql");

  /**
   * The {@code INSERT} of the migration itself, as {@code scripts/reset-demo-stock.sh} does: a
   * later seed change cannot leave the ITs restoring old values (review PR #5 A13).
   */
  static final String STOCK_SEED_INSERT = stockSeedInsert();

  private OrderTestData() {}

  static void restoreSeed(JdbcTemplate jdbc) {
    jdbc.update("delete from order_line");
    jdbc.update("delete from order_order");
    jdbc.update("delete from inventory_stock");
    jdbc.update(STOCK_SEED_INSERT);
  }

  private static String stockSeedInsert() {
    try {
      Matcher insert =
          Pattern.compile("(?s)INSERT INTO inventory_stock.*?;")
              .matcher(Files.readString(STOCK_MIGRATION));
      if (!insert.find()) {
        throw new IllegalStateException("No INSERT INTO inventory_stock in " + STOCK_MIGRATION);
      }
      return insert.group();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static void setStock(JdbcTemplate jdbc, String sku, int units) {
    jdbc.update("update inventory_stock set stock = ? where sku = ?", units, sku);
  }

  static int stock(JdbcTemplate jdbc, String sku) {
    return jdbc.queryForObject(
        "select stock from inventory_stock where sku = ?", Integer.class, sku);
  }

  static int orderCount(JdbcTemplate jdbc) {
    return jdbc.queryForObject("select count(*) from order_order", Integer.class);
  }

  static String body(String items) {
    return """
        {
          "firstName": "Ada",
          "lastName": "Lovelace",
          "storeId": "store-a",
          "items": %s
        }
        """
        .formatted(items);
  }

  static String line(String sku, int quantity) {
    return "{ \"variantId\": \"%s\", \"quantity\": %d }".formatted(sku, quantity);
  }
}
