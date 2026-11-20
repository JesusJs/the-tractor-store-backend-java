package com.tractorstore.catalog.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCategoryRepository extends JpaRepository<CategoryEntity, String> {

  List<CategoryEntity> findAllByOrderByPositionAsc();
}
