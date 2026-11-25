package com.tractorstore.order.application.service;

import com.tractorstore.order.application.port.in.GetOrderUseCase;
import com.tractorstore.order.domain.exception.OrderNotFoundException;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.model.OrderId;
import com.tractorstore.order.domain.port.out.OrderRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every miss is the same 404: invalid id, no session, other session or unknown order. */
@Service
@Transactional(readOnly = true)
class GetOrderService implements GetOrderUseCase {

  private final OrderRepository orders;

  GetOrderService(OrderRepository orders) {
    this.orders = orders;
  }

  @Override
  public Order get(String orderId, Optional<UUID> sessionId) {
    Optional<OrderId> id = OrderId.parse(orderId);
    if (id.isEmpty() || sessionId.isEmpty()) {
      throw new OrderNotFoundException();
    }
    return orders
        .findByIdAndSession(id.get(), sessionId.get())
        .orElseThrow(OrderNotFoundException::new);
  }
}
