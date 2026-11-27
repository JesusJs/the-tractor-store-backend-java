package com.tractorstore.notifications.application.port.in;

import java.util.UUID;

/** Inbound port: tell the buyer that their order was confirmed. */
public interface NotifyOrderPlacedUseCase {

  void notify(UUID orderId);
}
