package com.tractorstore.catalog.application.service;

import com.tractorstore.catalog.application.port.in.ListStoresUseCase;
import com.tractorstore.catalog.domain.model.Store;
import com.tractorstore.catalog.domain.port.out.StoreRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ListStoresService implements ListStoresUseCase {

  private final StoreRepository stores;

  ListStoresService(StoreRepository stores) {
    this.stores = stores;
  }

  @Override
  public List<Store> list() {
    return stores.findAll();
  }
}
