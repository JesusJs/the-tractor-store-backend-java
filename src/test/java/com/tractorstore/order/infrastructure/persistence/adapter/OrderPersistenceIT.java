package com.tractorstore.order.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tractorstore.TestcontainersConfiguration;
import com.tractorstore.order.domain.model.Money;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.model.OrderId;
import com.tractorstore.order.domain.model.OrderLine;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Orders written and read back on the real V5 schema (rolled back after each test). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, OrderRepositoryAdapter.class})
@ActiveProfiles("test")
class OrderPersistenceIT {

  private static final UUID SESSION = UUID.fromString("3f0c2a4e-8b1d-4c7a-9e5f-1a2b3c4d5e6f");
  private static final String CDN = "https://blueprint.the-tractor.store/cdn/img/product/400/";

  @Autowired private OrderRepositoryAdapter orders;
  @Autowired private EntityManager entityManager;

  private static OrderLine line(String sku, String price, int quantity) {
    return new OrderLine(
        sku,
        sku.substring(0, 5),
        "Product " + sku,
        "Variant " + sku,
        new Money(new BigDecimal(price)),
        quantity,
        CDN + sku + ".webp");
  }

  private static Order order(String extraPickups) {
    return Order.place(
        OrderId.newId(),
        SESSION,
        "Ada",
        "Lovelace",
        "store-a",
        extraPickups,
        List.of(
            line("CL-01-GR", "5700", 2), line("AU-07-YE", "5000", 1), line("AU-01-SI", "8700", 1)),
        Instant.parse("2026-11-24T21:15:03.123Z"));
  }

  private Optional<Order> saveAndReadBack(Order order, UUID session) {
    orders.save(order);
    entityManager.flush();
    entityManager.clear();
    return orders.findByIdAndSession(order.id(), session);
  }

  @Test
  void readsBackTheSameOrder() {
    Order saved = order("Big Micro Machines, Central Mall");

    Order read = saveAndReadBack(saved, SESSION).orElseThrow();

    assertThat(read.id()).isEqualTo(saved.id());
    assertThat(read.sessionId()).isEqualTo(SESSION);
    assertThat(read.firstName()).isEqualTo("Ada");
    assertThat(read.lastName()).isEqualTo("Lovelace");
    assertThat(read.storeId()).isEqualTo("store-a");
    assertThat(read.extraPickups()).isEqualTo("Big Micro Machines, Central Mall");
    assertThat(read.lines()).isEqualTo(saved.lines());
    assertThat(read.subTotal().amount()).isEqualTo(new BigDecimal("25100.00"));
    assertThat(read.tax().amount()).isEqualTo(new BigDecimal("5271.00"));
    assertThat(read.total().amount()).isEqualTo(new BigDecimal("30371.00"));
    assertThat(read.status()).isEqualTo(saved.status());
    assertThat(read.placedAt()).isEqualTo(Instant.parse("2026-11-24T21:15:03.123Z"));
  }

  @Test
  void linesKeepTheirPosition() {
    Order read = saveAndReadBack(order(null), SESSION).orElseThrow();

    assertThat(read.lines())
        .extracting(OrderLine::sku)
        .containsExactly("CL-01-GR", "AU-07-YE", "AU-01-SI");
  }

  @Test
  void missingExtraPickupsStaysNull() {
    assertThat(saveAndReadBack(order(null), SESSION).orElseThrow().extraPickups()).isNull();
  }

  @Test
  void anotherSessionFindsNothing() {
    assertThat(saveAndReadBack(order(null), UUID.randomUUID())).isEmpty();
  }

  @Test
  void unknownIdFindsNothing() {
    assertThat(orders.findByIdAndSession(OrderId.newId(), SESSION)).isEmpty();
  }
}
