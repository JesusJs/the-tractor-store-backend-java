package com.tractorstore.catalog.domain.exception;

import com.tractorstore.shared.error.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

  public ProductNotFoundException(String id) {
    super("PRODUCT_NOT_FOUND", "Product " + id + " not found");
  }
}
