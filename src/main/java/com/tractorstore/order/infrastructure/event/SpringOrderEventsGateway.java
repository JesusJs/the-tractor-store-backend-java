package com.tractorstore.order.infrastructure.event;

import com.tractorstore.order.api.OrderPlaced;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.port.out.OrderEventsGateway;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link OrderPlaced} as a Spring event; Spring Modulith stores it in the event
 * publication registry within the current transaction (research R13, R14).
 */
@Component
class SpringOrderEventsGateway implements OrderEventsGateway {

  private final ApplicationEventPublisher publisher;

  SpringOrderEventsGateway(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void orderPlaced(Order order) {
    publisher.publishEvent(
        new OrderPlaced(
            order.id().value(),
            order.sessionId(),
            order.lines().stream()
                .map(line -> new OrderPlaced.Line(line.sku(), line.quantity()))
                .toList()));
  }
}
