package com.tractorstore.order.domain.port.out;

import com.tractorstore.order.domain.model.Order;

/** Output port: tells the rest of the application that an order was confirmed. */
public interface OrderEventsGateway {

  /** Called inside the order's transaction: a rollback discards the event too. */
  void orderPlaced(Order order);
}
