package com.tractorstore.catalog.application.port.in;

import com.tractorstore.catalog.application.model.CategoryListing;

/** Inbound port: products of a category listing ({@code all}, {@code classic}, ...). */
public interface GetCategoryListingUseCase {

  /**
   * @throws com.tractorstore.catalog.domain.exception.CategoryNotFoundException if the filter is
   *     neither {@code all} nor an existing category code (exact match)
   */
  CategoryListing get(String filter);
}
