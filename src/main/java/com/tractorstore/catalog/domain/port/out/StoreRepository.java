package com.tractorstore.catalog.domain.port.out;

import com.tractorstore.catalog.domain.model.Store;
import java.util.List;

/** Output port: physical stores. */
public interface StoreRepository {

  /** All stores in dataset order. */
  List<Store> findAll();

  /** Exact (case sensitive) match on the store id. */
  boolean existsById(String id);
}
