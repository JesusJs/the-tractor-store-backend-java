package com.tractorstore.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tractorstore.inventory.domain.exception.StockNotFoundException;
import com.tractorstore.inventory.domain.model.StockItem;
import com.tractorstore.inventory.domain.port.out.StockRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetStockServiceTest {

  @Mock private StockRepository stock;
  @InjectMocks private GetStockService service;

  @Test
  void returnsTheStockOfAKnownSku() {
    StockItem item = new StockItem("CL-01-GR", 10);
    when(stock.findBySku("CL-01-GR")).thenReturn(Optional.of(item));

    assertThat(service.get("CL-01-GR")).isEqualTo(item);
  }

  @Test
  void unknownSkuIsVariantNotFound() {
    when(stock.findBySku("XX-99-ZZ")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get("XX-99-ZZ"))
        .isInstanceOf(StockNotFoundException.class)
        .hasMessage("Variant XX-99-ZZ not found")
        .extracting("code")
        .isEqualTo("VARIANT_NOT_FOUND");
  }
}
