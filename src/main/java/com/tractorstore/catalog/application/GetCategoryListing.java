package com.tractorstore.catalog.application;

/** Inbound port: products of a category listing ({@code all}, {@code classic}, ...). */
public interface GetCategoryListing {

  /**
   * @throws com.tractorstore.catalog.domain.CategoryNotFoundException if the filter is neither
   *     {@code all} nor an existing category code (exact match)
   */
  CategoryListing get(String filter);
}
