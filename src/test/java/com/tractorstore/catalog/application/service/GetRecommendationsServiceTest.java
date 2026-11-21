package com.tractorstore.catalog.application.service;

import static com.tractorstore.catalog.CatalogFixtures.au07;
import static com.tractorstore.catalog.CatalogFixtures.cl01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tractorstore.catalog.domain.model.Recommendation;
import com.tractorstore.catalog.domain.port.out.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetRecommendationsServiceTest {

  @Mock private ProductRepository products;
  @InjectMocks private GetRecommendationsService service;

  @Test
  void delegatesToThePolicyWithTheWholeCatalog() {
    when(products.findAll()).thenReturn(List.of(cl01(), au07()));

    List<Recommendation> recommendations = service.get(List.of("CL-01-GR"));

    // From olive #6B8E23: CL-01-GY 110.0, AU-07-YE 169.4, AU-07-MT 203.0 (one per product).
    assertThat(recommendations)
        .extracting(r -> r.variant().sku())
        .containsExactly("CL-01-GY", "AU-07-YE");
    verify(products, times(1)).findAll();
  }

  @Test
  void withoutSkusReturnsTheDefaultList() {
    when(products.findAll()).thenReturn(List.of(cl01(), au07()));

    assertThat(service.get(null))
        .extracting(r -> r.variant().sku())
        .containsExactly("CL-01-GR", "AU-07-MT");
    verify(products, times(1)).findAll();
  }
}
