package com.tractorstore.catalog.application;

import static com.tractorstore.catalog.CatalogFixtures.CLASSIC;
import static com.tractorstore.catalog.CatalogFixtures.au07;
import static com.tractorstore.catalog.CatalogFixtures.cl01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tractorstore.catalog.domain.CategoryNotFoundException;
import com.tractorstore.catalog.domain.port.CategoryRepository;
import com.tractorstore.catalog.domain.port.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCategoryListingServiceTest {

  @Mock private CategoryRepository categories;
  @Mock private ProductRepository products;
  @InjectMocks private GetCategoryListingService service;

  @Test
  void allListsEveryProduct() {
    when(products.findAll()).thenReturn(List.of(cl01(), au07()));

    CategoryListing listing = service.get("all");

    assertThat(listing.filter()).isEqualTo("all");
    assertThat(listing.products()).extracting("id").containsExactly("CL-01", "AU-07");
    verify(categories, never()).findByCode(anyString());
  }

  @Test
  void knownCategoryListsItsProducts() {
    when(categories.findByCode("classic")).thenReturn(Optional.of(CLASSIC));
    when(products.findByCategory("classic")).thenReturn(List.of(cl01()));

    CategoryListing listing = service.get("classic");

    assertThat(listing.filter()).isEqualTo("classic");
    assertThat(listing.products()).extracting("id").containsExactly("CL-01");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Classic", "classics", "tractors"})
  void unknownFilterIsCategoryNotFound(String filter) {
    when(categories.findByCode(filter)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(filter))
        .isInstanceOf(CategoryNotFoundException.class)
        .hasMessage("Category " + filter + " not found")
        .extracting("code")
        .isEqualTo("CATEGORY_NOT_FOUND");
    verify(products, never()).findByCategory(anyString());
  }
}
