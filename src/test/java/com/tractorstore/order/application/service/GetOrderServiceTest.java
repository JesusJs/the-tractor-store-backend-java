package com.tractorstore.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tractorstore.order.domain.exception.OrderNotFoundException;
import com.tractorstore.order.domain.model.Money;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.model.OrderId;
import com.tractorstore.order.domain.model.OrderLine;
import com.tractorstore.order.domain.port.out.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

  private static final UUID SESSION = UUID.fromString("3f0c2a4e-8b1d-4c7a-9e5f-1a2b3c4d5e6f");
  private static final String ID = "0b8e6a8e-5d0f-4a51-9d3c-2f7f3f1a7c11";

  @Mock private OrderRepository orders;
  @InjectMocks private GetOrderService service;

  @Test
  void idThatIsNotAUuidIsNotFoundWithoutTouchingTheRepository() {
    assertThatThrownBy(() -> service.get("1-1-1-1-1", Optional.of(SESSION)))
        .isInstanceOf(OrderNotFoundException.class);
    verifyNoInteractions(orders);
  }

  @Test
  void withoutSessionIsNotFound() {
    assertThatThrownBy(() -> service.get(ID, Optional.empty()))
        .isInstanceOf(OrderNotFoundException.class);
    verifyNoInteractions(orders);
  }

  @Test
  void orderOfAnotherSessionOrUnknownIsNotFoundWithAFixedMessage() {
    when(orders.findByIdAndSession(OrderId.parse(ID).orElseThrow(), SESSION))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID, Optional.of(SESSION)))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found")
        .extracting("code")
        .isEqualTo("ORDER_NOT_FOUND");
  }

  @Test
  void orderOfTheSessionIsReturned() {
    OrderId id = OrderId.parse(ID).orElseThrow();
    Order order =
        Order.place(
            id,
            SESSION,
            "Ada",
            "Lovelace",
            "store-a",
            null,
            List.of(
                new OrderLine(
                    "AU-07-YE",
                    "AU-07",
                    "Verde Voyager",
                    "Yellow",
                    new Money(new BigDecimal("5000")),
                    1,
                    "img")),
            Instant.parse("2026-11-24T21:15:03.123Z"));
    when(orders.findByIdAndSession(id, SESSION)).thenReturn(Optional.of(order));

    assertThat(service.get(ID, Optional.of(SESSION))).isSameAs(order);
  }
}
