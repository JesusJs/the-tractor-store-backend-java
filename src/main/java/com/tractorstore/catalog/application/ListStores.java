package com.tractorstore.catalog.application;

import com.tractorstore.catalog.domain.Store;
import java.util.List;

/** Inbound port: physical stores. */
public interface ListStores {

  /** Stores in dataset order. */
  List<Store> list();
}
