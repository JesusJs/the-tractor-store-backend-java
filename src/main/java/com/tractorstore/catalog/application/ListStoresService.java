package com.tractorstore.catalog.application;

import com.tractorstore.catalog.domain.Store;
import com.tractorstore.catalog.domain.port.StoreRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ListStoresService implements ListStores {

  private final StoreRepository stores;

  ListStoresService(StoreRepository stores) {
    this.stores = stores;
  }

  @Override
  public List<Store> list() {
    return stores.findAll();
  }
}
