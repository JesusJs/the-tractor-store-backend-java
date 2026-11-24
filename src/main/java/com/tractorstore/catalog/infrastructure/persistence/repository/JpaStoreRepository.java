package com.tractorstore.catalog.infrastructure.persistence.repository;

import com.tractorstore.catalog.infrastructure.persistence.entity.StoreEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaStoreRepository extends JpaRepository<StoreEntity, String> {

  List<StoreEntity> findAllByOrderByPositionAsc();
}
