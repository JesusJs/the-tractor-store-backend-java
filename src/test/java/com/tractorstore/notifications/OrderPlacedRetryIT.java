package com.tractorstore.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.tractorstore.TestcontainersConfiguration;
import com.tractorstore.notifications.application.port.in.NotifyOrderPlacedUseCase;
import com.tractorstore.order.api.OrderPlaced;
import com.tractorstore.order.application.model.PlaceOrderCommand;
import com.tractorstore.order.application.model.PlaceOrderCommand.Line;
import com.tractorstore.order.application.port.in.PlaceOrderUseCase;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;

/**
 * A failing listener leaves {@link OrderPlaced} pending in the registry; resubmitting it delivers
 * the notification and the order stays confirmed (AS-3.3, FR-024, SC-006).
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, OrderPlacedRetryIT.FailOnceConfiguration.class})
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class OrderPlacedRetryIT {

  private static final String SKU = "AU-07-YE";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  /** Only this listener's publication: another listener of OrderPlaced has its own row (C8). */
  private static final String LISTENER = "%OrderPlacedListener%";

  @Autowired private PlaceOrderUseCase placeOrder;
  @Autowired private IncompleteEventPublications incomplete;
  @Autowired private CompletedEventPublications completed;
  @Autowired private FailOnceNotifier notifier;
  @Autowired private JdbcTemplate jdbc;

  private int seedStock;
  private UUID orderId;

  /** Fails the first call and delegates the next ones to the real service (it writes the log). */
  static class FailOnceNotifier implements NotifyOrderPlacedUseCase {

    private final NotifyOrderPlacedUseCase real;
    private final AtomicInteger calls = new AtomicInteger();

    FailOnceNotifier(NotifyOrderPlacedUseCase real) {
      this.real = real;
    }

    @Override
    public void notify(UUID orderId) {
      if (calls.incrementAndGet() == 1) {
        throw new IllegalStateException("mail server down");
      }
      real.notify(orderId);
    }

    int calls() {
      return calls.get();
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FailOnceConfiguration {

    @Bean
    @Primary
    FailOnceNotifier failOnceNotifier(
        @Qualifier("notifyOrderPlacedService") NotifyOrderPlacedUseCase real) {
      return new FailOnceNotifier(real);
    }
  }

  @BeforeEach
  void ownStock() {
    seedStock =
        jdbc.queryForObject("select stock from inventory_stock where sku = ?", int.class, SKU);
    jdbc.update("update inventory_stock set stock = 5 where sku = ?", SKU);
  }

  @AfterEach
  void cleanUp() {
    if (orderId != null) {
      jdbc.update(
          "delete from event_publication where serialized_event like ?", "%" + orderId + "%");
      jdbc.update("delete from order_line where order_id = ?", orderId);
      jdbc.update("delete from order_order where id = ?", orderId);
    }
    jdbc.update("update inventory_stock set stock = ? where sku = ?", seedStock, SKU);
  }

  @Test
  void failedNotificationIsRetriedAndTheOrderIsKept(CapturedOutput output) {
    orderId =
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
    Predicate<EventPublication> ofThisOrder =
        publication ->
            publication.getEvent() instanceof OrderPlaced event && event.orderId().equals(orderId);

    await().atMost(TIMEOUT).until(() -> "FAILED".equals(status()));
    assertThat(notifier.calls()).isEqualTo(1);
    assertThat(output.getOut()).doesNotContain("Confirmation email sent for order " + orderId);

    incomplete.resubmitIncompletePublications(ofThisOrder);

    await().atMost(TIMEOUT).until(() -> completed.findAll().stream().anyMatch(ofThisOrder::test));
    assertThat(status()).isEqualTo("COMPLETED");
    // SC-006: exactly one notification despite the failed first attempt (review C1).
    assertThat(notifier.calls()).isEqualTo(2);
    assertThat(
            StringUtils.countOccurrencesOf(
                output.getOut(), "Confirmation email sent for order " + orderId))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from order_order where id = ?", Integer.class, orderId))
        .isEqualTo(1);
  }

  private String status() {
    return jdbc
        .queryForList(
            "select status from event_publication"
                + " where serialized_event like ? and listener_id like ?",
            String.class,
            "%" + orderId + "%",
            LISTENER)
        .stream()
        .findFirst()
        .orElse(null);
  }
}
