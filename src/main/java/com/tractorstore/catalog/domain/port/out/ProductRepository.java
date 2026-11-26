package com.tractorstore.catalog.domain.port.out;

import com.tractorstore.catalog.domain.model.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Output port: products with their category, highlights and variants. */
public interface ProductRepository {

  /** All products in dataset order. */
  List<Product> findAll();

  /** Products of one category in dataset order. */
  List<Product> findByCategory(String code);

  Optional<Product> findById(String id);

  /** Products having at least one of {@code skus}, each with all its variants. */
  List<Product> findByVariantSkus(Collection<String> skus);
}
