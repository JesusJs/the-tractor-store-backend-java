package com.tractorstore.catalog.domain.port;

import com.tractorstore.catalog.domain.Product;
import java.util.List;
import java.util.Optional;

/** Output port: products with their category, highlights and variants. */
public interface ProductRepository {

  /** All products in dataset order. */
  List<Product> findAll();

  /** Products of one category in dataset order. */
  List<Product> findByCategory(String code);

  Optional<Product> findById(String id);
}
