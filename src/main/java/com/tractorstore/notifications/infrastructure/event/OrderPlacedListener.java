package com.tractorstore.notifications.infrastructure.event;

import com.tractorstore.notifications.application.port.in.NotifyOrderPlacedUseCase;
import com.tractorstore.order.api.OrderPlaced;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Runs asynchronously after the order commits, in its own transaction; if it fails the publication
 * stays incomplete in {@code event_publication} for resubmission (FR-024, research R13).
 */
@Component
class OrderPlacedListener {

  private final NotifyOrderPlacedUseCase notifyOrderPlaced;

  OrderPlacedListener(NotifyOrderPlacedUseCase notifyOrderPlaced) {
    this.notifyOrderPlaced = notifyOrderPlaced;
  }

  @ApplicationModuleListener
  void on(OrderPlaced event) {
    notifyOrderPlaced.notify(event.orderId());
  }
}
