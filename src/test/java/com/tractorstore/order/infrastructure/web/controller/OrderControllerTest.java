package com.tractorstore.order.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tractorstore.order.application.model.PlaceOrderCommand;
import com.tractorstore.order.application.port.in.GetOrderUseCase;
import com.tractorstore.order.application.port.in.PlaceOrderUseCase;
import com.tractorstore.order.domain.exception.CartEmptyException;
import com.tractorstore.order.domain.exception.OrderNotFoundException;
import com.tractorstore.order.domain.exception.OutOfStockException;
import com.tractorstore.order.domain.exception.StoreNotFoundException;
import com.tractorstore.order.domain.exception.VariantNotFoundException;
import com.tractorstore.order.domain.model.Money;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.model.OrderId;
import com.tractorstore.order.domain.model.OrderLine;
import com.tractorstore.shared.error.BusinessException;
import com.tractorstore.shared.infrastructure.web.GlobalExceptionHandler;
import com.tractorstore.shared.infrastructure.web.JsonCoercionConfiguration;
import com.tractorstore.shared.infrastructure.web.SessionCookies;
import com.tractorstore.shared.infrastructure.web.TraceIdFilter;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(OrderController.class)
@Import({
  GlobalExceptionHandler.class,
  TraceIdFilter.class,
  SessionCookies.class,
  JsonCoercionConfiguration.class
})
class OrderControllerTest {

  private static final String ORDERS = "/api/v1/orders";
  private static final String SESSION = "3f0c2a4e-8b1d-4c7a-9e5f-1a2b3c4d5e6f";
  private static final String ORDER_ID = "0b8e6a8e-5d0f-4a51-9d3c-2f7f3f1a7c11";
  private static final String IMAGE =
      "https://blueprint.the-tractor.store/cdn/img/product/400/AU-07-YE.webp";

  /** Exact body sent by the front's {@code cart.service.ts} (contracts/orders-api.md). */
  private static final String FRONT_BODY =
      """
      {
        "firstName": "Ada",
        "lastName": "Lovelace",
        "storeId": "store-a",
        "extraPickups": "Big Micro Machines, Central Mall",
        "items": [
          {
            "productId": "AU-07",
            "variantId": "AU-07-YE",
            "productName": "Verde Voyager",
            "variantName": "AU-07-YE",
            "price": 4000,
            "quantity": 1,
            "image": "https://blueprint.the-tractor.store/cdn/img/product/[size]/AU-07-YE.webp"
          }
        ]
      }
      """;

  @Autowired private MockMvc mvc;

  @MockitoBean private PlaceOrderUseCase placeOrder;
  @MockitoBean private GetOrderUseCase getOrder;

  private static Order order(String extraPickups, Instant placedAt) {
    return Order.place(
        OrderId.parse(ORDER_ID).orElseThrow(),
        UUID.fromString(SESSION),
        "Ada",
        "Lovelace",
        "store-a",
        extraPickups,
        List.of(
            new OrderLine(
                "AU-07-YE",
                "AU-07",
                "Verde Voyager",
                "Yellow",
                new Money(new BigDecimal("5000")),
                1,
                IMAGE)),
        placedAt);
  }

  private static Order order() {
    return order("Big Micro Machines, Central Mall", Instant.parse("2026-11-24T21:15:03.123Z"));
  }

  private ResultActions postOrder(String body, Cookie... cookies) throws Exception {
    var request = post(ORDERS).contentType(MediaType.APPLICATION_JSON).content(body);
    if (cookies.length > 0) {
      request.cookie(cookies);
    }
    return mvc.perform(request);
  }

  private static void expectReceipt(ResultActions result) throws Exception {
    result
        .andExpect(jsonPath("$.id").value(ORDER_ID))
        .andExpect(jsonPath("$.firstName").value("Ada"))
        .andExpect(jsonPath("$.lastName").value("Lovelace"))
        .andExpect(jsonPath("$.storeId").value("store-a"))
        .andExpect(jsonPath("$.extraPickups[0]").value("Big Micro Machines"))
        .andExpect(jsonPath("$.extraPickups[1]").value("Central Mall"))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].productId").value("AU-07"))
        .andExpect(jsonPath("$.items[0].variantId").value("AU-07-YE"))
        .andExpect(jsonPath("$.items[0].productName").value("Verde Voyager"))
        .andExpect(jsonPath("$.items[0].variantName").value("AU-07-YE"))
        .andExpect(jsonPath("$.items[0].price").value(5000.00))
        .andExpect(jsonPath("$.items[0].quantity").value(1))
        .andExpect(jsonPath("$.items[0].image").value(IMAGE))
        .andExpect(jsonPath("$.subTotal").value(5000.00))
        .andExpect(jsonPath("$.tax").value(1050.00))
        .andExpect(jsonPath("$.total").value(6050.00))
        .andExpect(jsonPath("$.placedAt").value("2026-11-24T21:15:03.123Z"))
        .andExpect(jsonPath("$.status").value("PLACED"));
  }

  @Test
  void frontBodyPlacesTheOrder() throws Exception {
    when(placeOrder.place(any())).thenReturn(order());

    ResultActions result =
        postOrder(FRONT_BODY)
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, ORDERS + "/" + ORDER_ID))
            .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    expectReceipt(result);
  }

  @Test
  void onlySkuAndQuantityReachTheUseCaseWithTheCookieSession() throws Exception {
    when(placeOrder.place(any())).thenReturn(order());

    postOrder(FRONT_BODY, new Cookie("ts_session", SESSION))
        .andExpect(status().isCreated())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    ArgumentCaptor<PlaceOrderCommand> command = ArgumentCaptor.forClass(PlaceOrderCommand.class);
    verify(placeOrder).place(command.capture());
    assertThat(command.getValue())
        .isEqualTo(
            new PlaceOrderCommand(
                UUID.fromString(SESSION),
                "Ada",
                "Lovelace",
                "store-a",
                "Big Micro Machines, Central Mall",
                List.of(new PlaceOrderCommand.Line("AU-07-YE", 1))));
  }

  @Test
  void missingItemsReachTheUseCaseAsNull() throws Exception {
    when(placeOrder.place(any())).thenThrow(new CartEmptyException());

    postOrder(
            """
            { "firstName": "Ada", "lastName": "Lovelace", "storeId": "store-a" }
            """)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CART_EMPTY"));

    ArgumentCaptor<PlaceOrderCommand> command = ArgumentCaptor.forClass(PlaceOrderCommand.class);
    verify(placeOrder).place(command.capture());
    assertThat(command.getValue().lines()).isNull();
  }

  @Test
  void missingExtraPickupsIsAnEmptyList() throws Exception {
    when(placeOrder.place(any()))
        .thenReturn(order(null, Instant.parse("2026-11-24T21:15:03.123Z")));

    postOrder(FRONT_BODY)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.extraPickups").isArray())
        .andExpect(jsonPath("$.extraPickups.length()").value(0));
  }

  @Test
  void placedAtAlwaysHasMilliseconds() throws Exception {
    when(placeOrder.place(any())).thenReturn(order(null, Instant.parse("2026-11-24T21:15:03Z")));

    postOrder(FRONT_BODY)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.placedAt").value("2026-11-24T21:15:03.000Z"));
  }

  static Stream<Arguments> malformedBodies() {
    return Stream.of(
        Arguments.of(FRONT_BODY.replace("\"Big Micro Machines, Central Mall\"", "[\"a\"]")),
        Arguments.of(FRONT_BODY.replace("\"quantity\": 1", "\"quantity\": \"one\"")),
        Arguments.of(FRONT_BODY.replace("\"quantity\": 1", "\"quantity\": 1.5")),
        Arguments.of(FRONT_BODY.replace("\"quantity\": 1", "\"quantity\": \"2\"")),
        Arguments.of(FRONT_BODY.replace("\"variantId\": \"AU-07-YE\"", "\"variantId\": 12")),
        Arguments.of("{ not json"));
  }

  @ParameterizedTest
  @MethodSource("malformedBodies")
  void unreadableBodyIsMalformedRequest(String body) throws Exception {
    postOrder(body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.traceId").exists());
    verify(placeOrder, never()).place(any());
  }

  private static String items(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> "{ \"variantId\": \"AU-07-YE\", \"quantity\": 1 }")
        .collect(Collectors.joining(", ", "[", "]"));
  }

  static Stream<Arguments> invalidBodies() {
    String name101 = "a".repeat(101);
    return Stream.of(
        Arguments.of(
            FRONT_BODY.replace("\"variantId\": \"AU-07-YE\"", "\"variantId\": \"\""),
            "items[0].variantId"),
        Arguments.of(
            FRONT_BODY.replace(
                "\"variantId\": \"AU-07-YE\"", "\"variantId\": \"" + "A".repeat(21) + "\""),
            "items[0].variantId"),
        Arguments.of(FRONT_BODY.replace("\"quantity\": 1", "\"quantity\": 0"), "items[0].quantity"),
        Arguments.of(
            FRONT_BODY.replace("\"quantity\": 1", "\"quantity\": 100"), "items[0].quantity"),
        Arguments.of(FRONT_BODY.replace("\"quantity\": 1,", ""), "items[0].quantity"),
        Arguments.of(
            FRONT_BODY.replace("\"variantId\": \"AU-07-YE\"", "\"variantId\": \"AU-07\\u0000\""),
            "items[0].variantId"),
        Arguments.of(
            FRONT_BODY.replace("\"variantId\": \"AU-07-YE\"", "\"variantId\": \"AU 07\""),
            "items[0].variantId"),
        Arguments.of(FRONT_BODY.replace("\"Ada\"", "\"Ada\\u0000\""), "firstName"),
        Arguments.of(FRONT_BODY.replace("\"Lovelace\"", "\"Love\\nlace\""), "lastName"),
        Arguments.of(FRONT_BODY.replace("\"store-a\"", "\"store\\u0000\""), "storeId"),
        Arguments.of(FRONT_BODY.replace("Central Mall\"", "Central Mall\\u0007\""), "extraPickups"),
        Arguments.of(FRONT_BODY.replace("\"Ada\"", "\"  \""), "firstName"),
        Arguments.of(FRONT_BODY.replace("\"Ada\"", "\"" + name101 + "\""), "firstName"),
        Arguments.of(FRONT_BODY.replace("\"Lovelace\"", "\"\""), "lastName"),
        Arguments.of(FRONT_BODY.replace("\"Lovelace\"", "\"" + name101 + "\""), "lastName"),
        Arguments.of(FRONT_BODY.replace("\"store-a\"", "\" \""), "storeId"),
        Arguments.of(FRONT_BODY.replace("\"store-a\"", "\"" + "s".repeat(21) + "\""), "storeId"),
        Arguments.of(
            FRONT_BODY.replace(
                "\"Big Micro Machines, Central Mall\"", "\"" + "x".repeat(501) + "\""),
            "extraPickups"),
        Arguments.of(
            """
            { "firstName": "Ada", "lastName": "Lovelace", "storeId": "store-a", "items": %s }
            """
                .formatted(items(51)),
            "items"),
        Arguments.of(
            """
            { "firstName": "Ada", "lastName": "Lovelace", "storeId": "store-a",
              "items": [ { "variantId": "AU-07-YE", "quantity": 1 }, null ] }
            """,
            "items[1]"));
  }

  @ParameterizedTest
  @MethodSource("invalidBodies")
  void constraintViolationIsValidationErrorWithTheFieldPath(String body, String field)
      throws Exception {
    postOrder(body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details['" + field + "']").exists())
        .andExpect(jsonPath("$.traceId").exists());
    verify(placeOrder, never()).place(any());
  }

  @Test
  void fiftyLinesAreAccepted() throws Exception {
    when(placeOrder.place(any())).thenReturn(order());

    postOrder(
            """
            { "firstName": "Ada", "lastName": "Lovelace", "storeId": "store-a", "items": %s }
            """
                .formatted(items(50)))
        .andExpect(status().isCreated());
  }

  static Stream<Arguments> domainErrors() {
    return Stream.of(
        Arguments.of(new CartEmptyException(), 409),
        Arguments.of(new StoreNotFoundException("store-x"), 404),
        Arguments.of(new VariantNotFoundException("XX-99-ZZ"), 404),
        Arguments.of(new OutOfStockException("AU-08-WH"), 409));
  }

  @ParameterizedTest
  @MethodSource("domainErrors")
  void domainErrorsKeepTheirCodeAndStatus(BusinessException error, int status) throws Exception {
    when(placeOrder.place(any())).thenThrow(error);

    postOrder(FRONT_BODY)
        .andExpect(status().is(status))
        .andExpect(jsonPath("$.code").value(error.code()))
        .andExpect(jsonPath("$.message").value(error.getMessage()))
        .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void getWithTheCookieReturnsTheReceipt() throws Exception {
    when(getOrder.get(ORDER_ID, Optional.of(UUID.fromString(SESSION)))).thenReturn(order());

    ResultActions result =
        mvc.perform(get(ORDERS + "/" + ORDER_ID).cookie(new Cookie("ts_session", SESSION)))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    expectReceipt(result);
  }

  @Test
  void getWithoutCookieDoesNotIssueOne() throws Exception {
    when(getOrder.get(eq(ORDER_ID), eq(Optional.empty()))).thenThrow(new OrderNotFoundException());

    mvc.perform(get(ORDERS + "/" + ORDER_ID))
        .andExpect(status().isNotFound())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Order not found"))
        .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void getOfAnotherSessionIsOrderNotFound() throws Exception {
    when(getOrder.get(any(), any())).thenThrow(new OrderNotFoundException());

    mvc.perform(get(ORDERS + "/abc").cookie(new Cookie("ts_session", SESSION)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Order not found"));
  }
}
