package com.tractorstore.order;

import static com.tractorstore.order.OrderTestData.body;
import static com.tractorstore.order.OrderTestData.line;
import static com.tractorstore.order.OrderTestData.orderCount;
import static com.tractorstore.order.OrderTestData.stock;
import static org.assertj.core.api.Assertions.assertThat;

import com.tractorstore.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Two buyers race for the last unit: exactly one wins, no overselling (AS-2.12, SC-003). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class OrderConcurrencyIT {

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void lastUnit() {
    OrderTestData.setStock(jdbc, "AU-08-WH", 1);
  }

  @AfterEach
  void restoreSeed() {
    OrderTestData.restoreSeed(jdbc);
  }

  @RepeatedTest(5)
  void onlyOneOfTwoConcurrentOrdersGetsTheLastUnit() throws Exception {
    int before = orderCount(jdbc);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Integer> buyer =
        () -> {
          start.await();
          return client
              .post()
              .uri("/api/v1/orders")
              .contentType(MediaType.APPLICATION_JSON)
              .cookie("ts_session", UUID.randomUUID().toString())
              .body(body("[" + line("AU-08-WH", 1) + "]"))
              .exchange()
              .returnResult()
              .getStatus()
              .value();
        };

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<Integer>> results = List.of(pool.submit(buyer), pool.submit(buyer));
      start.countDown();

      List<Integer> statuses = List.of(results.get(0).get(), results.get(1).get());
      assertThat(statuses).containsExactlyInAnyOrder(201, 409);
    } finally {
      pool.shutdown();
      assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
    assertThat(stock(jdbc, "AU-08-WH")).isZero();
    assertThat(orderCount(jdbc)).isEqualTo(before + 1);
  }
}
