package com.tractorstore.order.application.port.in;

import com.tractorstore.order.domain.model.Order;
import java.util.Optional;
import java.util.UUID;

/** Inbound port: receipt of an order of the session. */
public interface GetOrderUseCase {

  /** Throws {@code OrderNotFoundException} for an invalid id, no session or another session. */
  Order get(String orderId, Optional<UUID> sessionId);
}
