package com.tractorstore.catalog.infrastructure.persistence;

import com.tractorstore.catalog.domain.Color;
import com.tractorstore.catalog.domain.Product;
import com.tractorstore.catalog.domain.Variant;
import com.tractorstore.catalog.domain.port.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

  private final JpaProductRepository products;

  ProductRepositoryAdapter(JpaProductRepository products) {
    this.products = products;
  }

  @Override
  public List<Product> findAll() {
    return toDomain(products.findAllByOrderByPositionAsc());
  }

  @Override
  public List<Product> findByCategory(String code) {
    return toDomain(products.findAllByCategoryCodeOrderByPositionAsc(code));
  }

  @Override
  public Optional<Product> findById(String id) {
    return products.findWithDetailsById(id).map(ProductRepositoryAdapter::toDomain);
  }

  private static List<Product> toDomain(List<ProductEntity> entities) {
    return entities.stream().map(ProductRepositoryAdapter::toDomain).toList();
  }

  private static Product toDomain(ProductEntity entity) {
    return new Product(
        entity.getId(),
        entity.getName(),
        CategoryRepositoryAdapter.toDomain(entity.getCategory()),
        entity.getHighlights(),
        entity.getVariants().stream().map(ProductRepositoryAdapter::toDomain).toList());
  }

  private static Variant toDomain(VariantEntity entity) {
    return new Variant(
        entity.getSku(),
        entity.getName(),
        Color.fromHex(entity.getColor()),
        entity.getPrice(),
        entity.getImage());
  }
}
