package com.tractorstore.order.infrastructure.web.controller;

import com.tractorstore.order.application.port.in.GetOrderUseCase;
import com.tractorstore.order.application.port.in.PlaceOrderUseCase;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.infrastructure.web.dto.OrderReceiptResponse;
import com.tractorstore.order.infrastructure.web.dto.PlaceOrderRequest;
import com.tractorstore.order.infrastructure.web.mapper.OrderDtoMapper;
import com.tractorstore.shared.infrastructure.web.ApiError;
import com.tractorstore.shared.infrastructure.web.SessionCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checkout and receipt. {@code POST} issues {@code ts_session} when missing, before the use case,
 * so a 404, 409 or 500 also carries it; a 400 does not, because Spring rejects the body before this
 * controller runs (contracts/orders-api.md). {@code GET} only reads it.
 */
@RestController
@RequestMapping(OrderPaths.BASE)
@Tag(name = "Orders", description = "Order confirmation and receipt of the session")
class OrderController {

  private final PlaceOrderUseCase placeOrder;
  private final GetOrderUseCase getOrder;
  private final SessionCookies sessionCookies;

  OrderController(
      PlaceOrderUseCase placeOrder, GetOrderUseCase getOrder, SessionCookies sessionCookies) {
    this.placeOrder = placeOrder;
    this.getOrder = getOrder;
    this.sessionCookies = sessionCookies;
  }

  @PostMapping
  @Operation(
      summary = "Place an order",
      description =
          "Confirms the order all or nothing: real catalog prices, 21 % VAT and an atomic stock"
              + " decrease per line. Item fields other than variantId and quantity are ignored;"
              + " taken from the catalog.")
  @ApiResponse(
      responseCode = "201",
      description = "Order placed",
      headers = {
        @Header(name = "Location", description = "/api/v1/orders/{id}"),
        @Header(
            name = "Set-Cookie",
            description = "ts_session, only when the request had no valid one; never on a 400")
      })
  @ApiResponse(
      responseCode = "400",
      description = "Malformed body (MALFORMED_REQUEST) or invalid field (VALIDATION_ERROR)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Unknown store (STORE_NOT_FOUND) or variant (VARIANT_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "409",
      description = "No items (CART_EMPTY) or not enough stock (OUT_OF_STOCK)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected error; nothing is saved",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  ResponseEntity<OrderReceiptResponse> place(
      @Valid @RequestBody PlaceOrderRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    UUID session = sessionCookies.resolveOrIssue(request, response);
    Order order = placeOrder.place(OrderDtoMapper.command(session, body));
    return ResponseEntity.created(URI.create(OrderPaths.BASE + "/" + order.id()))
        .body(OrderDtoMapper.receipt(order));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Receipt of an order",
      description =
          "Same receipt as the POST, only for the session that placed it. Never issues a cookie.")
  @ApiResponse(responseCode = "200", description = "Receipt")
  @ApiResponse(
      responseCode = "404",
      description = "Unknown order, another session, no session or invalid id (ORDER_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected error",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  OrderReceiptResponse get(@PathVariable String id, HttpServletRequest request) {
    return OrderDtoMapper.receipt(getOrder.get(id, sessionCookies.read(request)));
  }
}
