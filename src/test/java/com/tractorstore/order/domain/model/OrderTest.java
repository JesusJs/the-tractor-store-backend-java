package com.tractorstore.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {

  private static final UUID SESSION = UUID.fromString("3f0c2a4e-8b1d-4c7a-9e5f-1a2b3c4d5e6f");
  private static final Instant PLACED_AT = Instant.parse("2026-11-24T21:15:03.123Z");

  static OrderLine line(String sku, String price, int quantity) {
    return new OrderLine(
        sku,
        sku.substring(0, 5),
        "Product " + sku,
        "Variant " + sku,
        new Money(new BigDecimal(price)),
        quantity,
        "https://blueprint.the-tractor.store/cdn/img/product/400/" + sku + ".webp");
  }

  static Order place(String extraPickups, List<OrderLine> lines) {
    return Order.place(
        OrderId.newId(), SESSION, "Ada", "Lovelace", "store-a", extraPickups, lines, PLACED_AT);
  }

  @Test
  void placeComputesSubTotalTaxAndTotal() {
    Order order =
        place(null, List.of(line("AU-07-YE", "5000.00", 1), line("CL-01-GR", "5700.00", 2)));

    assertThat(order.subTotal()).isEqualTo(new Money(new BigDecimal("16400.00")));
    assertThat(order.tax()).isEqualTo(new Money(new BigDecimal("3444.00")));
    assertThat(order.total()).isEqualTo(new Money(new BigDecimal("19844.00")));
    assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
    assertThat(order.placedAt()).isEqualTo(PLACED_AT);
    assertThat(order.sessionId()).isEqualTo(SESSION);
    assertThat(order.firstName()).isEqualTo("Ada");
    assertThat(order.lastName()).isEqualTo("Lovelace");
    assertThat(order.storeId()).isEqualTo("store-a");
  }

  @Test
  void linesKeepTheReceivedOrderAndAreAnImmutableCopy() {
    List<OrderLine> lines =
        new ArrayList<>(List.of(line("CL-01-GR", "5700", 1), line("AU-07-YE", "5000", 1)));
    Order order = place(null, lines);
    lines.clear();

    assertThat(order.lines()).extracting(OrderLine::sku).containsExactly("CL-01-GR", "AU-07-YE");
    assertThatThrownBy(() -> order.lines().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void linesMustNotBeEmpty() {
    assertThatThrownBy(() -> place(null, List.of())).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> place(null, null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void lineTotalIsUnitPriceTimesQuantity() {
    assertThat(line("AU-07-YE", "5000.00", 3).lineTotal())
        .isEqualTo(new Money(new BigDecimal("15000.00")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = "  ")
  void lineTextsMustNotBeBlank(String text) {
    Money price = new Money(BigDecimal.TEN);
    assertThatThrownBy(() -> new OrderLine(text, "p", "pn", "vn", price, 1, "img"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OrderLine("s", text, "pn", "vn", price, 1, "img"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OrderLine("s", "p", text, "vn", price, 1, "img"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OrderLine("s", "p", "pn", text, price, 1, "img"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OrderLine("s", "p", "pn", "vn", price, 1, text))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void unitPriceMustBePositive() {
    assertThatThrownBy(() -> line("AU-07-YE", "0", 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OrderLine("s", "p", "pn", "vn", null, 1, "img"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 100})
  void quantityMustBeBetweenOneAndNinetyNine(int quantity) {
    assertThatThrownBy(() -> line("AU-07-YE", "5000", quantity))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 99})
  void quantityLimitsAreAllowed(int quantity) {
    assertThat(line("AU-07-YE", "5000", quantity).quantity()).isEqualTo(quantity);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = "  ")
  void blankExtraPickupsIsAnEmptyList(String extraPickups) {
    assertThat(place(extraPickups, List.of(line("AU-07-YE", "5000", 1))).extraPickupList())
        .isEmpty();
  }

  @Test
  void extraPickupsAreSplitByCommaAndSpace() {
    assertThat(place("Store A, Store B", List.of(line("AU-07-YE", "5000", 1))).extraPickupList())
        .containsExactly("Store A", "Store B");
  }

  @Test
  void emptyFragmentsAreDropped() {
    assertThat(place("a, ", List.of(line("AU-07-YE", "5000", 1))).extraPickupList())
        .containsExactly("a");
  }

  @Test
  void extraPickupsAreKeptAsReceived() {
    assertThat(place("a, ", List.of(line("AU-07-YE", "5000", 1))).extraPickups()).isEqualTo("a, ");
  }

  @Test
  void belongsOnlyToItsSession() {
    Order order = place(null, List.of(line("AU-07-YE", "5000", 1)));

    assertThat(order.belongsTo(SESSION)).isTrue();
    assertThat(order.belongsTo(UUID.randomUUID())).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void customerTextsMustNotBeBlank(String text) {
    List<OrderLine> lines = List.of(line("AU-07-YE", "5000", 1));

    assertThatThrownBy(
            () ->
                Order.place(
                    OrderId.newId(), SESSION, text, "Lovelace", "store-a", null, lines, PLACED_AT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("firstName must not be blank");
    assertThatThrownBy(
            () ->
                Order.place(
                    OrderId.newId(), SESSION, "Ada", text, "store-a", null, lines, PLACED_AT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("lastName must not be blank");
    assertThatThrownBy(
            () ->
                Order.place(
                    OrderId.newId(), SESSION, "Ada", "Lovelace", text, null, lines, PLACED_AT))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("storeId must not be blank");
  }

  @Test
  void restoreKeepsTheStoredAmounts() {
    Money stored = new Money(new BigDecimal("1.00"));
    OrderId id = OrderId.newId();

    Order order =
        Order.restore(
            id,
            SESSION,
            "Ada",
            "Lovelace",
            "store-a",
            null,
            List.of(line("AU-07-YE", "5000", 1)),
            stored,
            stored,
            stored,
            OrderStatus.PLACED,
            PLACED_AT);

    assertThat(order.id()).isEqualTo(id);
    assertThat(order.subTotal()).isEqualTo(stored);
    assertThat(order.tax()).isEqualTo(stored);
    assertThat(order.total()).isEqualTo(stored);
  }
}
