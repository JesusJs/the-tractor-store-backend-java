package com.tractorstore.order.application.service;

import com.tractorstore.catalog.api.CatalogApi;
import com.tractorstore.catalog.api.VariantView;
import com.tractorstore.inventory.api.InventoryApi;
import com.tractorstore.order.application.model.PlaceOrderCommand;
import com.tractorstore.order.application.model.PlaceOrderCommand.Line;
import com.tractorstore.order.application.port.in.PlaceOrderUseCase;
import com.tractorstore.order.domain.exception.CartEmptyException;
import com.tractorstore.order.domain.exception.OutOfStockException;
import com.tractorstore.order.domain.exception.StoreNotFoundException;
import com.tractorstore.order.domain.exception.VariantNotFoundException;
import com.tractorstore.order.domain.model.Money;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.model.OrderId;
import com.tractorstore.order.domain.model.OrderLine;
import com.tractorstore.order.domain.port.out.OrderEventsGateway;
import com.tractorstore.order.domain.port.out.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms an order in a single transaction (research R9): any failure after a stock decrease rolls
 * every decrease back, so nothing is half done. {@code OrderPlaced} is published in that same
 * transaction, so a rolled back order publishes nothing (research R13).
 */
@Service
class PlaceOrderService implements PlaceOrderUseCase {

  /** Locks stock rows always in the same order, so concurrent orders cannot deadlock. */
  private static final Comparator<Line> BY_SKU = Comparator.comparing(Line::sku);

  private final CatalogApi catalog;
  private final InventoryApi inventory;
  private final OrderRepository orders;
  private final OrderEventsGateway events;
  private final Clock clock;

  PlaceOrderService(
      CatalogApi catalog,
      InventoryApi inventory,
      OrderRepository orders,
      OrderEventsGateway events,
      Clock clock) {
    this.catalog = catalog;
    this.inventory = inventory;
    this.orders = orders;
    this.events = events;
    this.clock = clock;
  }

  @Override
  @Transactional
  public Order place(PlaceOrderCommand command) {
    List<Line> lines = command.lines();
    if (lines == null || lines.isEmpty()) {
      throw new CartEmptyException();
    }
    if (!catalog.storeExists(command.storeId())) {
      throw new StoreNotFoundException(command.storeId());
    }
    Map<String, VariantView> variants = variantsOf(lines);
    Order order =
        Order.place(
            OrderId.newId(),
            command.sessionId(),
            command.firstName(),
            command.lastName(),
            command.storeId(),
            command.extraPickups(),
            lines.stream().map(line -> toOrderLine(line, variants.get(line.sku()))).toList(),
            Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
    lines.stream().sorted(BY_SKU).forEach(this::decreaseStock);
    orders.save(order);
    events.orderPlaced(order);
    return order;
  }

  /** One catalog query for all lines; the first unknown SKU in line order is reported. */
  private Map<String, VariantView> variantsOf(List<Line> lines) {
    List<String> skus = lines.stream().map(Line::sku).distinct().toList();
    Map<String, VariantView> variants =
        catalog.findVariants(skus).stream()
            .collect(Collectors.toMap(VariantView::sku, Function.identity()));
    skus.stream()
        .filter(sku -> !variants.containsKey(sku))
        .findFirst()
        .ifPresent(
            sku -> {
              throw new VariantNotFoundException(sku);
            });
    return variants;
  }

  private void decreaseStock(Line line) {
    if (!inventory.decreaseStock(line.sku(), line.quantity())) {
      throw new OutOfStockException(line.sku());
    }
  }

  private static OrderLine toOrderLine(Line line, VariantView variant) {
    return new OrderLine(
        variant.sku(),
        variant.productId(),
        variant.productName(),
        variant.variantName(),
        new Money(variant.price()),
        line.quantity(),
        variant.image());
  }
}
