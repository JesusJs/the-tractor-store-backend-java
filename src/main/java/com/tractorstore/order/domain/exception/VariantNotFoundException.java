package com.tractorstore.order.domain.exception;

import com.tractorstore.shared.error.NotFoundException;

/** Order line whose SKU is not a catalog variant. */
public class VariantNotFoundException extends NotFoundException {

  public VariantNotFoundException(String sku) {
    super("VARIANT_NOT_FOUND", "Variant " + sku + " not found");
  }
}
