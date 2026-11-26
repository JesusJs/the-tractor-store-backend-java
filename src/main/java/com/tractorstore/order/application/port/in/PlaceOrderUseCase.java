package com.tractorstore.order.application.port.in;

import com.tractorstore.order.application.model.PlaceOrderCommand;
import com.tractorstore.order.domain.model.Order;

/** Inbound port: confirm an order, all or nothing. */
public interface PlaceOrderUseCase {

  Order place(PlaceOrderCommand command);
}
