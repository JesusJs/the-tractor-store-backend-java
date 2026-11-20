package com.tractorstore.catalog.domain;

import com.tractorstore.shared.error.NotFoundException;

/** Reserved for the cart (spec 007); no catalog endpoint throws it. */
public class VariantNotFoundException extends NotFoundException {

  public VariantNotFoundException(String sku) {
    super("VARIANT_NOT_FOUND", "Variant " + sku + " not found");
  }
}
