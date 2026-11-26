package com.tractorstore.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tractorstore.catalog.api.VariantView;
import com.tractorstore.catalog.application.port.in.FindVariantsUseCase;
import com.tractorstore.catalog.application.port.in.StoreExistsUseCase;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogApiAdapterTest {

  @Mock private FindVariantsUseCase findVariants;
  @Mock private StoreExistsUseCase storeExists;
  @InjectMocks private CatalogApiAdapter api;

  @Test
  void findVariantsDelegatesToTheUseCase() {
    VariantView view =
        new VariantView("AU-07-YE", "AU-07", "Verde Voyager", "Yellow", BigDecimal.TEN, "img");
    when(findVariants.find(List.of("AU-07-YE"))).thenReturn(List.of(view));

    assertThat(api.findVariants(List.of("AU-07-YE"))).containsExactly(view);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void storeExistsDelegatesToTheUseCase(boolean exists) {
    when(storeExists.exists("store-a")).thenReturn(exists);

    assertThat(api.storeExists("store-a")).isEqualTo(exists);
  }
}
