package com.tractorstore.catalog.application.service;

import static com.tractorstore.catalog.CatalogFixtures.au07;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tractorstore.catalog.domain.exception.ProductNotFoundException;
import com.tractorstore.catalog.domain.port.out.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetProductDetailServiceTest {

  @Mock private ProductRepository products;
  @InjectMocks private GetProductDetailService service;

  @Test
  void existingIdReturnsTheProduct() {
    when(products.findById("AU-07")).thenReturn(Optional.of(au07()));

    assertThat(service.get("AU-07").id()).isEqualTo("AU-07");
  }

  @Test
  void unknownIdIsProductNotFound() {
    when(products.findById("CL-99")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get("CL-99"))
        .isInstanceOf(ProductNotFoundException.class)
        .hasMessage("Product CL-99 not found")
        .extracting("code")
        .isEqualTo("PRODUCT_NOT_FOUND");
  }
}
