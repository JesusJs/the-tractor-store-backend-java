package com.tractorstore.order.domain.exception;

import com.tractorstore.shared.error.NotFoundException;

/** Pickup store that is not a catalog store. */
public class StoreNotFoundException extends NotFoundException {

  public StoreNotFoundException(String storeId) {
    super("STORE_NOT_FOUND", "Store " + storeId + " not found");
  }
}
