package com.tractorstore.catalog.application.service;

import com.tractorstore.catalog.application.model.CategoryListing;
import com.tractorstore.catalog.application.port.in.GetCategoryListingUseCase;
import com.tractorstore.catalog.domain.exception.CategoryNotFoundException;
import com.tractorstore.catalog.domain.model.CategoryFilter;
import com.tractorstore.catalog.domain.model.Product;
import com.tractorstore.catalog.domain.port.out.CategoryRepository;
import com.tractorstore.catalog.domain.port.out.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetCategoryListingService implements GetCategoryListingUseCase {

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
