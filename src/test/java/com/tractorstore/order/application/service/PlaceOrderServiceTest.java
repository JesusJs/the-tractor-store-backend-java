package com.tractorstore.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tractorstore.catalog.api.CatalogApi;
import com.tractorstore.catalog.api.VariantView;
import com.tractorstore.inventory.api.InventoryApi;
import com.tractorstore.order.application.model.PlaceOrderCommand;
import com.tractorstore.order.application.model.PlaceOrderCommand.Line;
import com.tractorstore.order.domain.exception.CartEmptyException;
import com.tractorstore.order.domain.exception.OutOfStockException;
import com.tractorstore.order.domain.exception.StoreNotFoundException;
import com.tractorstore.order.domain.exception.VariantNotFoundException;
import com.tractorstore.order.domain.model.Money;
import com.tractorstore.order.domain.model.Order;
import com.tractorstore.order.domain.model.OrderLine;
import com.tractorstore.order.domain.model.OrderStatus;
import com.tractorstore.order.domain.port.out.OrderEventsGateway;
import com.tractorstore.order.domain.port.out.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

  private static final UUID SESSION = UUID.fromString("3f0c2a4e-8b1d-4c7a-9e5f-1a2b3c4d5e6f");
  private static final Instant NOW = Instant.parse("2026-11-24T21:15:03.123456789Z");
  private static final String CDN = "https://blueprint.the-tractor.store/cdn/img/product/400/";

  private static final VariantView AU_07_YE =
      new VariantView(
          "AU-07-YE",
          "AU-07",
          "Verde Voyager",
          "Yellow",
          new BigDecimal("5000.00"),
          CDN + "AU-07-YE.webp");
  private static final VariantView CL_01_GR =
      new VariantView(
          "CL-01-GR",
          "CL-01",
          "Heritage Workhorse",
          "Green",
          new BigDecimal("5700.00"),
          CDN + "CL-01-GR.webp");

  @Mock private CatalogApi catalog;
  @Mock private InventoryApi inventory;
  @Mock private OrderRepository orders;
  @Mock private OrderEventsGateway events;

  private PlaceOrderService service;

  @BeforeEach
  void setUp() {
    service =
        new PlaceOrderService(catalog, inventory, orders, events, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static PlaceOrderCommand command(List<Line> lines) {
    return new PlaceOrderCommand(
        SESSION, "Ada", "Lovelace", "store-a", "Big Micro Machines", lines);
  }

  private void catalogKnows(VariantView... views) {
    when(catalog.storeExists("store-a")).thenReturn(true);
    when(catalog.findVariants(anyCollection())).thenReturn(List.of(views));
  }

  @Test
  void nullLinesIsCartEmpty() {
    assertThatThrownBy(() -> service.place(command(null))).isInstanceOf(CartEmptyException.class);
    verifyNoInteractions(catalog, inventory, orders, events);
  }

  @Test
  void noLinesIsCartEmpty() {
    assertThatThrownBy(() -> service.place(command(List.of())))
        .isInstanceOf(CartEmptyException.class);
    verifyNoInteractions(catalog, inventory, orders, events);
  }

  @Test
  void unknownStoreIsStoreNotFoundBeforeLookingAtVariants() {
    when(catalog.storeExists("store-a")).thenReturn(false);

    assertThatThrownBy(() -> service.place(command(List.of(new Line("XX-99-ZZ", 1)))))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessage("Store store-a not found");
    verify(catalog, never()).findVariants(anyCollection());
    verifyNoInteractions(inventory, orders, events);
  }

  @Test
  void firstUnknownSkuInLineOrderIsVariantNotFound() {
    catalogKnows(AU_07_YE);

    assertThatThrownBy(
            () ->
                service.place(
                    command(
                        List.of(
                            new Line("AU-07-YE", 1),
                            new Line("ZZ-02-XX", 1),
                            new Line("AA-01-XX", 1)))))
        .isInstanceOf(VariantNotFoundException.class)
        .hasMessage("Variant ZZ-02-XX not found");
    verifyNoInteractions(inventory, orders, events);
  }

  @Test
  @SuppressWarnings("unchecked")
  void variantsAreLookedUpOnceWithDistinctSkus() {
    catalogKnows(AU_07_YE, CL_01_GR);
    when(inventory.decreaseStock(anyString(), anyInt())).thenReturn(true);

    service.place(
        command(
            List.of(new Line("AU-07-YE", 1), new Line("CL-01-GR", 1), new Line("AU-07-YE", 2))));

    ArgumentCaptor<Collection<String>> skus = ArgumentCaptor.forClass(Collection.class);
    verify(catalog, times(1)).findVariants(skus.capture());
    assertThat(skus.getValue()).containsExactlyInAnyOrder("AU-07-YE", "CL-01-GR");
  }

  @Test
  void stockIsDecreasedPerLineSortedBySku() {
    catalogKnows(AU_07_YE, CL_01_GR);
    when(inventory.decreaseStock(anyString(), anyInt())).thenReturn(true);

    service.place(
        command(
            List.of(new Line("CL-01-GR", 1), new Line("AU-07-YE", 2), new Line("AU-07-YE", 3))));

    InOrder order = inOrder(inventory);
    order.verify(inventory).decreaseStock("AU-07-YE", 2);
    order.verify(inventory).decreaseStock("AU-07-YE", 3);
    order.verify(inventory).decreaseStock("CL-01-GR", 1);
  }

  @Test
  void lineWithoutStockIsOutOfStockAndNothingIsSaved() {
    catalogKnows(AU_07_YE, CL_01_GR);
    when(inventory.decreaseStock("AU-07-YE", 1)).thenReturn(true);
    when(inventory.decreaseStock("CL-01-GR", 1)).thenReturn(false);

    assertThatThrownBy(
            () -> service.place(command(List.of(new Line("CL-01-GR", 1), new Line("AU-07-YE", 1)))))
        .isInstanceOf(OutOfStockException.class)
        .hasMessage("Variant CL-01-GR is out of stock");
    verify(orders, never()).save(any());
    verifyNoInteractions(events);
  }

  @Test
  @DisplayName("regresión: precio enviado por el cliente")
  void linesUseTheCatalogData() {
    catalogKnows(AU_07_YE);
    when(inventory.decreaseStock(anyString(), anyInt())).thenReturn(true);

    Order order = service.place(command(List.of(new Line("AU-07-YE", 1))));

    assertThat(order.lines())
        .containsExactly(
            new OrderLine(
                "AU-07-YE",
                "AU-07",
                "Verde Voyager",
                "Yellow",
                new Money(new BigDecimal("5000")),
                1,
                CDN + "AU-07-YE.webp"));
    assertThat(order.subTotal()).isEqualTo(new Money(new BigDecimal("5000.00")));
    assertThat(order.tax()).isEqualTo(new Money(new BigDecimal("1050.00")));
    assertThat(order.total()).isEqualTo(new Money(new BigDecimal("6050.00")));
  }

  @Test
  void placedOrderIsSavedAndReturned() {
    catalogKnows(AU_07_YE, CL_01_GR);
    when(inventory.decreaseStock(anyString(), anyInt())).thenReturn(true);

    Order order = service.place(command(List.of(new Line("CL-01-GR", 2), new Line("AU-07-YE", 1))));

    verify(orders).save(order);
    assertThat(order.sessionId()).isEqualTo(SESSION);
    assertThat(order.firstName()).isEqualTo("Ada");
    assertThat(order.lastName()).isEqualTo("Lovelace");
    assertThat(order.storeId()).isEqualTo("store-a");
    assertThat(order.extraPickups()).isEqualTo("Big Micro Machines");
    assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
    assertThat(order.lines()).extracting(OrderLine::sku).containsExactly("CL-01-GR", "AU-07-YE");
  }

  @Test
  void placedAtIsTheClockTruncatedToMilliseconds() {
    catalogKnows(AU_07_YE);
    when(inventory.decreaseStock(anyString(), anyInt())).thenReturn(true);

    Order order = service.place(command(List.of(new Line("AU-07-YE", 1))));

    assertThat(order.placedAt()).isEqualTo(Instant.parse("2026-11-24T21:15:03.123Z"));
  }

  @Test
  void persistenceFailureIsPropagatedSoTheTransactionRollsBack() {
    catalogKnows(AU_07_YE);
    when(inventory.decreaseStock(anyString(), anyInt())).thenReturn(true);
    RuntimeException databaseDown = new RuntimeException("database down");
    org.mockito.Mockito.doThrow(databaseDown).when(orders).save(any());

    assertThatThrownBy(() -> service.place(command(List.of(new Line("AU-07-YE", 1)))))
        .isSameAs(databaseDown);
    verifyNoInteractions(events);
  }

  @Test
  void placedOrderIsPublishedOnceAfterItIsSaved() {
    catalogKnows(AU_07_YE);
    when(inventory.decreaseStock(anyString(), anyInt())).thenReturn(true);

    Order order = service.place(command(List.of(new Line("AU-07-YE", 1))));

    InOrder saveThenPublish = inOrder(orders, events);
    saveThenPublish.verify(orders).save(order);
    saveThenPublish.verify(events, times(1)).orderPlaced(order);
    saveThenPublish.verifyNoMoreInteractions();
  }
}
