package com.tractorstore.catalog.infrastructure.persistence.adapter;

import com.tractorstore.catalog.domain.model.Category;
import com.tractorstore.catalog.domain.port.out.CategoryRepository;
import com.tractorstore.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.tractorstore.catalog.infrastructure.persistence.repository.JpaCategoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class CategoryRepositoryAdapter implements CategoryRepository {

  private final JpaCategoryRepository categories;

  CategoryRepositoryAdapter(JpaCategoryRepository categories) {
    this.categories = categories;
  }

  @Override
  public List<Category> findAll() {
    return categories.findAllByOrderByPositionAsc().stream()
        .map(CategoryRepositoryAdapter::toDomain)
        .toList();
  }

  @Override
  public Optional<Category> findByCode(String code) {
    return categories.findById(code).map(CategoryRepositoryAdapter::toDomain);
  }

  static Category toDomain(CategoryEntity entity) {
    return new Category(
        entity.getCode(), entity.getBrandLabel(), entity.getTeaserTitle(), entity.getTeaserImage());
  }
}
