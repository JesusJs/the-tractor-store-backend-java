package com.tractorstore.catalog.infrastructure.persistence.repository;

import com.tractorstore.catalog.infrastructure.persistence.entity.CategoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCategoryRepository extends JpaRepository<CategoryEntity, String> {

  List<CategoryEntity> findAllByOrderByPositionAsc();
}
