package com.tractorstore.catalog.application.port.in;

import com.tractorstore.catalog.domain.model.Store;
import java.util.List;

/** Inbound port: physical stores. */
public interface ListStoresUseCase {

  /** Stores in dataset order. */
  List<Store> list();
}
