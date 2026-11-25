package com.tractorstore.catalog.application.service;

import static com.tractorstore.catalog.CatalogFixtures.au07;
import static com.tractorstore.catalog.CatalogFixtures.cl01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.tractorstore.catalog.api.VariantView;
import com.tractorstore.catalog.domain.port.out.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindVariantsServiceTest {

  @Mock private ProductRepository products;
  @InjectMocks private FindVariantsService service;

  @Test
  @DisplayName("regresión: precio enviado por el cliente (usa el precio real de la variante)")
  void viewHasTheRealVariantPriceNotTheFromPrice() {
    when(products.findByVariantSkus(List.of("AU-07-YE"))).thenReturn(List.of(au07()));

    List<VariantView> views = service.find(List.of("AU-07-YE"));

    assertThat(views)
        .containsExactly(
            new VariantView(
                "AU-07-YE",
                "AU-07",
                "Verde Voyager",
                "Name AU-07-YE",
                new BigDecimal("5000.00"),
                "https://blueprint.the-tractor.store/cdn/img/product/400/AU-07-YE.webp"));
  }

  @Test
  void onlyRequestedSkusAreReturned() {
    when(products.findByVariantSkus(anyCollection())).thenReturn(List.of(au07(), cl01()));

    assertThat(service.find(List.of("AU-07-MT", "CL-01-GY")))
        .extracting(VariantView::sku)
        .containsExactlyInAnyOrder("AU-07-MT", "CL-01-GY");
  }

  @Test
  void unknownSkusAreOmitted() {
    when(products.findByVariantSkus(anyCollection())).thenReturn(List.of(au07()));

    assertThat(service.find(List.of("AU-07-YE", "XX-99-ZZ")))
        .extracting(VariantView::sku)
        .containsExactly("AU-07-YE");
  }

  @Test
  void noMatchIsEmpty() {
    when(products.findByVariantSkus(anyCollection())).thenReturn(List.of());

    assertThat(service.find(List.of("XX-99-ZZ"))).isEmpty();
  }
}
