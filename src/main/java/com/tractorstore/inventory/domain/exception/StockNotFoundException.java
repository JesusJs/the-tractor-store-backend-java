package com.tractorstore.inventory.domain.exception;

import com.tractorstore.shared.error.NotFoundException;

/** No stock row for the SKU: for the API the variant does not exist. */
public class StockNotFoundException extends NotFoundException {

  public StockNotFoundException(String sku) {
    super("VARIANT_NOT_FOUND", "Variant " + sku + " not found");
  }
}
