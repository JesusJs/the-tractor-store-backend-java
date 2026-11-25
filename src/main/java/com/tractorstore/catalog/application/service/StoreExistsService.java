package com.tractorstore.catalog.application.service;

import com.tractorstore.catalog.application.port.in.StoreExistsUseCase;
import com.tractorstore.catalog.domain.port.out.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class StoreExistsService implements StoreExistsUseCase {

  private final StoreRepository stores;

  StoreExistsService(StoreRepository stores) {
    this.stores = stores;
  }

  @Override
  public boolean exists(String storeId) {
    return stores.existsById(storeId);
  }
}
