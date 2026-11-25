package com.tractorstore.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tractorstore.inventory.application.port.in.DecreaseStockUseCase;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryApiAdapterTest {

  @Mock private DecreaseStockUseCase decreaseStock;
  @InjectMocks private InventoryApiAdapter api;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void delegatesToTheUseCase(boolean decreased) {
    when(decreaseStock.decrease("AU-07-YE", 2)).thenReturn(decreased);

    assertThat(api.decreaseStock("AU-07-YE", 2)).isEqualTo(decreased);
  }
}
