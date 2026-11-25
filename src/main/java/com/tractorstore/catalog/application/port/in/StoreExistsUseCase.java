package com.tractorstore.catalog.application.port.in;

/** Inbound port: whether a store id belongs to the catalog. */
public interface StoreExistsUseCase {

  boolean exists(String storeId);
}
