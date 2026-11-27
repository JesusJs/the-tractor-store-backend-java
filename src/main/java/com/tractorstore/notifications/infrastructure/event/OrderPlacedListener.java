package com.tractorstore.notifications.infrastructure.event;

import com.tractorstore.notifications.application.port.in.NotifyOrderPlacedUseCase;
import com.tractorstore.order.api.OrderPlaced;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Runs asynchronously after the order commits, in its own transaction; if it fails the publication
 * stays incomplete in {@code event_publication} for resubmission (FR-024, research R13).
 *
 * <p>Accepted limitation (review C9): delivery is at-least-once and this listener is not
 * idempotent. A crash between the notification and the completion of the publication, or a
 * republication on restart, repeats it. Today that only duplicates a log line; a real email must
 * deduplicate by {@code orderId} first (constitution VII: not before it is needed).
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
