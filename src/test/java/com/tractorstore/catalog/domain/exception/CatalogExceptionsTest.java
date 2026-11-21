package com.tractorstore.catalog.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.tractorstore.shared.error.BusinessException;
import com.tractorstore.shared.error.ErrorKind;
import com.tractorstore.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;

class CatalogExceptionsTest {

  @Test
  void productNotFound() {
    ProductNotFoundException error = new ProductNotFoundException("CL-99");

    assertThat(error.code()).isEqualTo("PRODUCT_NOT_FOUND");
    assertThat(error.getMessage()).isEqualTo("Product CL-99 not found");
    assertThat(error.kind()).isEqualTo(ErrorKind.NOT_FOUND);
    assertThat(error).isInstanceOf(NotFoundException.class).isInstanceOf(BusinessException.class);
  }

  @Test
  void categoryNotFound() {
    CategoryNotFoundException error = new CategoryNotFoundException("classics");

    assertThat(error.code()).isEqualTo("CATEGORY_NOT_FOUND");
    assertThat(error.getMessage()).isEqualTo("Category classics not found");
    assertThat(error.kind()).isEqualTo(ErrorKind.NOT_FOUND);
    assertThat(error).isInstanceOf(NotFoundException.class);
  }

  @Test
  void variantNotFound() {
    VariantNotFoundException error = new VariantNotFoundException("AU-05-ZZ");

    assertThat(error.code()).isEqualTo("VARIANT_NOT_FOUND");
    assertThat(error.getMessage()).isEqualTo("Variant AU-05-ZZ not found");
    assertThat(error.kind()).isEqualTo(ErrorKind.NOT_FOUND);
    assertThat(error).isInstanceOf(NotFoundException.class);
  }
}
