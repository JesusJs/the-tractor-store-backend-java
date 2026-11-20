package com.tractorstore.catalog.application;

import com.tractorstore.catalog.domain.CategoryFilter;
import com.tractorstore.catalog.domain.CategoryNotFoundException;
import com.tractorstore.catalog.domain.Product;
import com.tractorstore.catalog.domain.port.CategoryRepository;
import com.tractorstore.catalog.domain.port.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetCategoryListingService implements GetCategoryListing {

  private final CategoryRepository categories;
  private final ProductRepository products;

  GetCategoryListingService(CategoryRepository categories, ProductRepository products) {
    this.categories = categories;
    this.products = products;
  }

  @Override
  public CategoryListing get(String filter) {
    CategoryFilter categoryFilter = CategoryFilter.of(filter);
    List<Product> listed =
        categoryFilter
            .categoryCode()
            .map(this::productsOfExistingCategory)
            .orElseGet(products::findAll);
    return new CategoryListing(categoryFilter.value(), listed);
  }

  private List<Product> productsOfExistingCategory(String code) {
    categories.findByCode(code).orElseThrow(() -> new CategoryNotFoundException(code));
    return products.findByCategory(code);
  }
}
