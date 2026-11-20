package com.tractorstore.catalog.infrastructure.persistence;

import com.tractorstore.catalog.domain.Store;
import com.tractorstore.catalog.domain.port.StoreRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StoreRepositoryAdapter implements StoreRepository {

  private final JpaStoreRepository stores;

  StoreRepositoryAdapter(JpaStoreRepository stores) {
    this.stores = stores;
  }

  @Override
  public List<Store> findAll() {
    return stores.findAllByOrderByPositionAsc().stream()
        .map(
            entity ->
                new Store(
                    entity.getId(),
                    entity.getName(),
                    entity.getAddress(),
                    entity.getCity(),
                    entity.getImage()))
        .toList();
  }
}
