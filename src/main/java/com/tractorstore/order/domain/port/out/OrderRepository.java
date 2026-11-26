package com.tractorstore.order.domain.port.out;

import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.model.OrderId;
import java.util.Optional;
import java.util.UUID;

/** Output port: confirmed orders. */
public interface OrderRepository {

  void save(Order order);

  /** The order only if it belongs to {@code sessionId}. */
  Optional<Order> findByIdAndSession(OrderId id, UUID sessionId);
}
