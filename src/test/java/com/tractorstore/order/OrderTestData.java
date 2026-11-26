package com.tractorstore.order;

import org.springframework.jdbc.core.JdbcTemplate;

/** Shared state of the order ITs: they commit, so they must leave the migrated data as it was. */
final class OrderTestData {

  /** Same values as {@code V4__inventory_stock.sql}. */
  static final String RESTORE_STOCK_SEED =
      "update inventory_stock set stock = case sku when 'CL-01-GY' then 0 when 'AU-08-WH' then 1"
          + " when 'AU-07-YE' then 5 else 10 end";

  private OrderTestData() {}

  static void restoreSeed(JdbcTemplate jdbc) {
    jdbc.update("delete from order_line");
    jdbc.update("delete from order_order");
    jdbc.update(RESTORE_STOCK_SEED);
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
