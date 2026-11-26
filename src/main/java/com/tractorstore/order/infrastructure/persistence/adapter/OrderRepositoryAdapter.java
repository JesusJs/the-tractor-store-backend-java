package com.tractorstore.order.infrastructure.persistence.adapter;

import com.tractorstore.order.domain.model.Money;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.model.OrderId;
import com.tractorstore.order.domain.model.OrderLine;
import com.tractorstore.order.domain.model.OrderStatus;
import com.tractorstore.order.domain.port.out.OrderRepository;
import com.tractorstore.order.infrastructure.persistence.entity.OrderEntity;
import com.tractorstore.order.infrastructure.persistence.entity.OrderLineEmbeddable;
import com.tractorstore.order.infrastructure.persistence.repository.JpaOrderRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OrderRepositoryAdapter implements OrderRepository {

  private final JpaOrderRepository orders;

  OrderRepositoryAdapter(JpaOrderRepository orders) {
    this.orders = orders;
  }

  @Override
  public void save(Order order) {
    orders.save(toEntity(order));
  }

  @Override
  public Optional<Order> findByIdAndSession(OrderId id, UUID sessionId) {
    return orders.findByIdAndSessionId(id.value(), sessionId).map(OrderRepositoryAdapter::toDomain);
  }

  private static OrderEntity toEntity(Order order) {
    return new OrderEntity(
        order.id().value(),
        order.sessionId(),
        order.firstName(),
        order.lastName(),
        order.storeId(),
        order.extraPickups(),
        order.lines().stream()
            .map(
                line ->
                    new OrderLineEmbeddable(
                        line.sku(),
                        line.productId(),
                        line.productName(),
                        line.variantName(),
                        line.unitPrice().amount(),
                        line.quantity(),
                        line.image()))
            .toList(),
        order.subTotal().amount(),
        order.tax().amount(),
        order.total().amount(),
        order.status().name(),
        order.placedAt());
  }

  private static Order toDomain(OrderEntity entity) {
    return Order.restore(
        new OrderId(entity.getId()),
        entity.getSessionId(),
        entity.getFirstName(),
        entity.getLastName(),
        entity.getStoreId(),
        entity.getExtraPickups(),
        entity.getLines().stream()
            .map(
                line ->
                    new OrderLine(
                        line.getSku(),
                        line.getProductId(),
                        line.getProductName(),
                        line.getVariantName(),
                        new Money(line.getUnitPrice()),
                        line.getQuantity(),
                        line.getImage()))
            .toList(),
        new Money(entity.getSubTotal()),
        new Money(entity.getTax()),
        new Money(entity.getTotal()),
        OrderStatus.valueOf(entity.getStatus()),
        entity.getPlacedAt());
  }
}
