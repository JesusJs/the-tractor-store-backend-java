package com.tractorstore.catalog.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaStoreRepository extends JpaRepository<StoreEntity, String> {

  List<StoreEntity> findAllByOrderByPositionAsc();
}
