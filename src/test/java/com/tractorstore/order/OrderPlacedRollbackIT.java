package com.tractorstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.tractorstore.TestcontainersConfiguration;
import com.tractorstore.order.api.OrderPlaced;
import com.tractorstore.order.application.model.PlaceOrderCommand;
import com.tractorstore.order.application.model.PlaceOrderCommand.Line;
import com.tractorstore.order.application.port.in.PlaceOrderUseCase;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A rollback after {@link OrderPlaced} was published discards the event too: no row in the registry
 * and no notification (AS-3.2, FR-022, review C2). Full context: notifications must be listening
 * for the publication to be stored at all.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class OrderPlacedRollbackIT {

  private static final String SKU = "AU-07-YE";

  @Autowired private PlaceOrderUseCase placeOrder;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void ownStock() {
    OrderTestData.setStock(jdbc, SKU, 5);
  }

  @AfterEach
  void restoreSeed() {
    OrderTestData.restoreSeed(jdbc);
  }

  @Test
  void rollbackAfterPublishingLeavesNoPublicationNoOrderAndNoNotification(CapturedOutput output) {
    UUID orderId =
        new TransactionTemplate(transactionManager)
            .execute(
                status -> {
                  UUID id =
                      placeOrder
                          .place(
                              new PlaceOrderCommand(
                                  UUID.randomUUID(),
                                  "Ada",
                                  "Lovelace",
                                  "store-a",
                                  null,
                                  List.of(new Line(SKU, 1))))
                          .id()
                          .value();
                  status.setRollbackOnly();
                  return id;
                });

    // The listener is asynchronous: give a wrongly committed event time to show up.
    await()
        .during(Duration.ofSeconds(1))
        .atMost(Duration.ofSeconds(2))
        .until(() -> !output.getOut().contains("Confirmation email sent for order " + orderId));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from event_publication where serialized_event like ?",
                Integer.class,
                "%" + orderId + "%"))
        .isZero();
    assertThat(OrderTestData.orderCount(jdbc)).isZero();
    assertThat(OrderTestData.stock(jdbc, SKU)).isEqualTo(5);
  }
}
