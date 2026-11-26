package com.tractorstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tractorstore.TestcontainersConfiguration;
import com.tractorstore.order.api.OrderPlaced;
import com.tractorstore.order.application.model.PlaceOrderCommand;
import com.tractorstore.order.application.model.PlaceOrderCommand.Line;
import com.tractorstore.order.application.port.in.PlaceOrderUseCase;
import com.tractorstore.order.domain.exception.OutOfStockException;
import com.tractorstore.order.domain.model.Order;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;

/**
 * The order module publishes {@link OrderPlaced} once per confirmed order and never for a failed
 * one (US3, AS-3.1, AS-3.2). {@code ALL_DEPENDENCIES}: it needs the beans of catalog and inventory.
 */
@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrderPlacedEventIT {

  private static final UUID SESSION = UUID.fromString("7a1e5c2d-9b3f-4e8a-a6d4-2c1b0f9e8d7c");

  @Autowired private PlaceOrderUseCase placeOrder;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void ownStock() {
    OrderTestData.setStock(jdbc, "AU-07-YE", 5);
    OrderTestData.setStock(jdbc, "CL-01-GR", 10);
    OrderTestData.setStock(jdbc, "CL-01-GY", 0);
  }

  @AfterEach
  void restoreSeed() {
    OrderTestData.restoreSeed(jdbc);
  }

  private static PlaceOrderCommand command(Line... lines) {
    return new PlaceOrderCommand(SESSION, "Ada", "Lovelace", "store-a", null, List.of(lines));
  }

  @Test
  void placedOrderPublishesOneOrderPlacedWithItsIdSessionAndLines(Scenario scenario) {
    scenario
        .stimulate(
            () -> placeOrder.place(command(new Line("CL-01-GR", 2), new Line("AU-07-YE", 1))))
        .andWaitForEventOfType(OrderPlaced.class)
        .toArriveAndVerify(
            (OrderPlaced event, Order order) -> {
              assertThat(event.orderId()).isEqualTo(order.id().value());
              assertThat(event.sessionId()).isEqualTo(SESSION);
              assertThat(event.lines())
                  .containsExactly(
                      new OrderPlaced.Line("CL-01-GR", 2), new OrderPlaced.Line("AU-07-YE", 1));
            });
  }

  @Test
  void eachConfirmedOrderPublishesExactlyOneEvent(PublishedEvents events) {
    Order order = placeOrder.place(command(new Line("AU-07-YE", 1)));

    assertThat(events.ofType(OrderPlaced.class))
        .singleElement()
        .extracting(OrderPlaced::orderId)
        .isEqualTo(order.id().value());
  }

  @Test
  void orderWithoutStockPublishesNothing(PublishedEvents events) {
    assertThatThrownBy(() -> placeOrder.place(command(new Line("CL-01-GY", 1))))
        .isInstanceOf(OutOfStockException.class);

    assertThat(events.ofType(OrderPlaced.class)).isEmpty();
  }
}
