package com.tractorstore.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tractorstore.inventory.domain.port.out.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DecreaseStockServiceTest {

  @Mock private StockRepository stock;
  @InjectMocks private DecreaseStockService service;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void returnsWhatTheConditionalUpdateReports(boolean decreased) {
    when(stock.decreaseIfAvailable("AU-08-WH", 1)).thenReturn(decreased);

    assertThat(service.decrease("AU-08-WH", 1)).isEqualTo(decreased);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void quantityBelowOneIsRejectedWithoutTouchingTheStock(int quantity) {
    assertThatThrownBy(() -> service.decrease("AU-08-WH", quantity))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(stock);
  }

  @Test
  void quantityOfOneIsAccepted() {
    when(stock.decreaseIfAvailable("CL-01-GR", 1)).thenReturn(true);

    assertThat(service.decrease("CL-01-GR", 1)).isTrue();
  }
}
