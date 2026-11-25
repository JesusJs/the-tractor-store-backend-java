package com.tractorstore.order.infrastructure.web.mapper;

import com.tractorstore.order.application.model.PlaceOrderCommand;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.model.OrderLine;
import com.tractorstore.order.infrastructure.web.dto.OrderReceiptItemResponse;
import com.tractorstore.order.infrastructure.web.dto.OrderReceiptResponse;
import com.tractorstore.order.infrastructure.web.dto.PlaceOrderRequest;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** HTTP ↔ application mapping (data-model §8). */
public final class OrderDtoMapper {

  /** Always three fraction digits, also for whole seconds. */
  private static final DateTimeFormatter PLACED_AT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneOffset.UTC);

  private OrderDtoMapper() {}

  /** Only SKU and quantity of each item; {@code null} items stay {@code null} (CART_EMPTY). */
  public static PlaceOrderCommand command(UUID sessionId, PlaceOrderRequest request) {
    return new PlaceOrderCommand(
        sessionId,
        request.firstName(),
        request.lastName(),
        request.storeId(),
        request.extraPickups(),
        request.items() == null
            ? null
            : request.items().stream()
                .map(item -> new PlaceOrderCommand.Line(item.variantId(), item.quantity()))
                .toList());
  }

  public static OrderReceiptResponse receipt(Order order) {
    return new OrderReceiptResponse(
        order.id().toString(),
        order.firstName(),
        order.lastName(),
        order.storeId(),
        order.extraPickupList(),
        order.lines().stream().map(OrderDtoMapper::item).toList(),
        order.subTotal().amount(),
        order.tax().amount(),
        order.total().amount(),
        PLACED_AT.format(order.placedAt()),
        order.status().name());
  }

  private static OrderReceiptItemResponse item(OrderLine line) {
    return new OrderReceiptItemResponse(
        line.productId(),
        line.sku(),
        line.productName(),
        line.sku(),
        line.unitPrice().amount(),
        line.quantity(),
        line.image());
  }
}
