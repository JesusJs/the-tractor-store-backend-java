package com.tractorstore.catalog.domain.port;

import com.tractorstore.catalog.domain.Category;
import java.util.List;
import java.util.Optional;

/** Output port: product categories. */
public interface CategoryRepository {

  /** All categories in dataset order. */
  List<Category> findAll();

  Optional<Category> findByCode(String code);
}
