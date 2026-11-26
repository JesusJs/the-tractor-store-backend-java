package com.tractorstore.order;

import static com.tractorstore.order.OrderTestData.body;
import static com.tractorstore.order.OrderTestData.line;
import static com.tractorstore.order.OrderTestData.orderCount;
import static com.tractorstore.order.OrderTestData.setStock;
import static com.tractorstore.order.OrderTestData.stock;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.tractorstore.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/** End to end: checkout and receipt over HTTP on the real database (US2, AS-2.1..2.11). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class OrderApiIT {

  private static final String ORDERS = "/api/v1/orders";
  private static final String COOKIE_VALUE = "ts_session=([0-9a-f-]{36});.*";

  /** Exact body sent by the front's {@code cart.service.ts}, with a stale price of 4000. */
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

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void ownStock() {
    // Own values (research R15); the seed is restored afterwards.
    OrderTestData.setStock(jdbc, "AU-07-YE", 5);
    OrderTestData.setStock(jdbc, "CL-01-GR", 10);
    OrderTestData.setStock(jdbc, "CL-01-GY", 0);
    OrderTestData.setStock(jdbc, "AU-08-WH", 1);
  }

  @AfterEach
  void restoreSeed() {
    OrderTestData.restoreSeed(jdbc);
  }

  private EntityExchangeResult<String> post(String body, String session) {
    var request = client.post().uri(ORDERS).contentType(MediaType.APPLICATION_JSON).body(body);
    if (session != null) {
      request.cookie("ts_session", session);
    }
    return request.exchange().expectBody(String.class).returnResult();
  }

  private EntityExchangeResult<String> get(String id, String session) {
    var request = client.get().uri(ORDERS + "/" + id);
    if (session != null) {
      request.cookie("ts_session", session);
    }
    return request.exchange().expectBody(String.class).returnResult();
  }

  private static String issuedSession(EntityExchangeResult<String> result) {
    String setCookie = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(setCookie).matches(COOKIE_VALUE);
    return setCookie.replaceAll(COOKIE_VALUE, "$1");
  }

  private static String code(EntityExchangeResult<String> result) {
    return JsonPath.read(result.getResponseBody(), "$.code");
  }

  private void assertNothingChanged(int ordersBefore) {
    assertThat(orderCount(jdbc)).isEqualTo(ordersBefore);
    assertThat(stock(jdbc, "AU-07-YE")).isEqualTo(5);
    assertThat(stock(jdbc, "CL-01-GR")).isEqualTo(10);
    assertThat(stock(jdbc, "CL-01-GY")).isZero();
    assertThat(stock(jdbc, "AU-08-WH")).isEqualTo(1);
  }

  @Test
  void frontBodyPlacesTheOrderAndDecreasesTheStock() {
    EntityExchangeResult<String> result = post(FRONT_BODY, null);

    assertThat(result.getStatus().value()).isEqualTo(201);
    String id = JsonPath.read(result.getResponseBody(), "$.id");
    assertThat(result.getResponseHeaders().getLocation()).hasToString(ORDERS + "/" + id);
    issuedSession(result);
    assertThat(stock(jdbc, "AU-07-YE")).isEqualTo(4);
    assertThat(JsonPath.<List<String>>read(result.getResponseBody(), "$.extraPickups"))
        .containsExactly("Big Micro Machines", "Central Mall");
    assertThat(JsonPath.<String>read(result.getResponseBody(), "$.status")).isEqualTo("PLACED");
  }

  @Test
  @DisplayName("regresión: precio enviado por el cliente")
  void priceSentByTheClientIsIgnored() {
    String body = post(FRONT_BODY, null).getResponseBody();

    assertThat(JsonPath.<Double>read(body, "$.items[0].price")).isEqualTo(5000.00);
    assertThat(JsonPath.<Double>read(body, "$.subTotal")).isEqualTo(5000.00);
    assertThat(JsonPath.<Double>read(body, "$.tax")).isEqualTo(1050.00);
    assertThat(JsonPath.<Double>read(body, "$.total")).isEqualTo(6050.00);
    assertThat(body).contains("\"subTotal\":5000.00", "\"tax\":1050.00", "\"total\":6050.00");
  }

  @Test
  @DisplayName("regresión: id de pedido consultable y líneas con SKU")
  void receiptOfTheSameSessionIsIdenticalToThePost() {
    String session = UUID.randomUUID().toString();
    EntityExchangeResult<String> placed = post(FRONT_BODY, session);
    String id = JsonPath.read(placed.getResponseBody(), "$.id");

    EntityExchangeResult<String> read = get(id, session);

    assertThat(read.getStatus().value()).isEqualTo(200);
    assertThat(read.getResponseHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
    assertThat(read.getResponseBody()).isEqualTo(placed.getResponseBody());
    assertThat(JsonPath.<String>read(read.getResponseBody(), "$.items[0].variantName"))
        .isEqualTo("AU-07-YE");
    assertThat(JsonPath.<String>read(read.getResponseBody(), "$.items[0].productName"))
        .isEqualTo("Verde Voyager");
  }

  @Test
  void receiptIsOnlyForItsSession() {
    String session = UUID.randomUUID().toString();
    String id = JsonPath.read(post(FRONT_BODY, session).getResponseBody(), "$.id");

    Map<String, EntityExchangeResult<String>> misses =
        Map.of(
            "other cookie", get(id, UUID.randomUUID().toString()),
            "no cookie", get(id, null),
            "invalid cookie", get(id, "1-1-1-1-1"),
            "unknown id", get(UUID.randomUUID().toString(), session),
            "id not uuid", get("abc", session));

    misses.forEach(
        (name, result) -> {
          assertThat(result.getStatus().value()).as(name).isEqualTo(404);
          assertThat(code(result)).as(name).isEqualTo("ORDER_NOT_FOUND");
          assertThat(JsonPath.<String>read(result.getResponseBody(), "$.message"))
              .as(name)
              .isEqualTo("Order not found");
          assertThat(result.getResponseHeaders().get(HttpHeaders.SET_COOKIE)).as(name).isNull();
        });
  }

  @Test
  void missingExtraPickupsIsAnEmptyList() {
    String body = post(body("[" + line("CL-01-GR", 1) + "]"), null).getResponseBody();

    assertThat(JsonPath.<List<String>>read(body, "$.extraPickups")).isEmpty();
  }

  @Test
  void noItemsIsCartEmpty() {
    int before = orderCount(jdbc);

    EntityExchangeResult<String> result = post(body("[]"), null);

    assertThat(result.getStatus().value()).isEqualTo(409);
    assertThat(code(result)).isEqualTo("CART_EMPTY");
    assertNothingChanged(before);
  }

  @Test
  void invalidFieldIsValidationError() {
    int before = orderCount(jdbc);

    EntityExchangeResult<String> result = post(body("[" + line("AU-07-YE", 0) + "]"), null);

    assertThat(result.getStatus().value()).isEqualTo(400);
    assertThat(code(result)).isEqualTo("VALIDATION_ERROR");
    assertThat(JsonPath.<String>read(result.getResponseBody(), "$.details['items[0].quantity']"))
        .isNotBlank();
    assertNothingChanged(before);
  }

  @Test
  void wrongJsonTypesAreMalformedRequestAndNotCoerced() {
    int before = orderCount(jdbc);

    Map<String, EntityExchangeResult<String>> results =
        Map.of(
            "float quantity",
            post(body("[{ \"variantId\": \"AU-07-YE\", \"quantity\": 1.5 }]"), null),
            "text quantity",
            post(body("[{ \"variantId\": \"AU-07-YE\", \"quantity\": \"2\" }]"), null),
            "number sku",
            post(body("[{ \"variantId\": 12, \"quantity\": 1 }]"), null));

    results.forEach(
        (name, result) -> {
          assertThat(result.getStatus().value()).as(name).isEqualTo(400);
          assertThat(code(result)).as(name).isEqualTo("MALFORMED_REQUEST");
        });
    assertNothingChanged(before);
  }

  @Test
  void unknownVariantIsVariantNotFound() {
    int before = orderCount(jdbc);

    EntityExchangeResult<String> result =
        post(body("[" + line("AU-07-YE", 1) + ", " + line("XX-99-ZZ", 1) + "]"), null);

    assertThat(result.getStatus().value()).isEqualTo(404);
    assertThat(code(result)).isEqualTo("VARIANT_NOT_FOUND");
    assertNothingChanged(before);
  }

  @Test
  void unknownStoreIsStoreNotFound() {
    int before = orderCount(jdbc);

    EntityExchangeResult<String> result =
        post(body("[" + line("AU-07-YE", 1) + "]").replace("store-a", "store-x"), null);

    assertThat(result.getStatus().value()).isEqualTo(404);
    assertThat(code(result)).isEqualTo("STORE_NOT_FOUND");
    assertNothingChanged(before);
  }

  @Test
  void lastLineWithoutStockRollsBackTheWholeOrder() {
    int before = orderCount(jdbc);

    EntityExchangeResult<String> result =
        post(
            body(
                "["
                    + line("CL-01-GR", 2)
                    + ", "
                    + line("AU-07-YE", 1)
                    + ", "
                    + line("CL-01-GY", 1)
                    + "]"),
            null);

    assertThat(result.getStatus().value()).isEqualTo(409);
    assertThat(code(result)).isEqualTo("OUT_OF_STOCK");
    assertNothingChanged(before);
  }

  @Test
  void linesOfTheSameSkuAreAddedUp() {
    int before = orderCount(jdbc);

    EntityExchangeResult<String> result =
        post(body("[" + line("AU-08-WH", 1) + ", " + line("AU-08-WH", 1) + "]"), null);

    assertThat(result.getStatus().value()).isEqualTo(409);
    assertThat(code(result)).isEqualTo("OUT_OF_STOCK");
    assertNothingChanged(before);
  }

  @Test
  void validationWinsOverOtherErrors() {
    setStock(jdbc, "AU-08-WH", 0);
    int before = orderCount(jdbc);

    EntityExchangeResult<String> result =
        post(body("[" + line("", 1) + ", " + line("AU-08-WH", 1) + "]"), null);

    assertThat(result.getStatus().value()).isEqualTo(400);
    assertThat(code(result)).isEqualTo("VALIDATION_ERROR");
    assertThat(orderCount(jdbc)).isEqualTo(before);
    assertThat(stock(jdbc, "AU-08-WH")).isZero();
  }

  @Test
  void invalidCookieGetsANewSessionThatOwnsTheOrder() {
    EntityExchangeResult<String> placed = post(FRONT_BODY, "1-1-1-1-1");
    String session = issuedSession(placed);
    String id = JsonPath.read(placed.getResponseBody(), "$.id");

    assertThat(session).isNotEqualTo("1-1-1-1-1");
    assertThat(get(id, session).getStatus().value()).isEqualTo(200);
  }
}
