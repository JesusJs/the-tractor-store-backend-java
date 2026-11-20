package com.tractorstore.catalog.domain.port;

import com.tractorstore.catalog.domain.Store;
import java.util.List;

/** Output port: physical stores. */
public interface StoreRepository {

  /** All stores in dataset order. */
  List<Store> findAll();
}
