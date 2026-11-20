package com.tractorstore.catalog.application;

import com.tractorstore.catalog.domain.Product;
import java.util.List;

/** Products of a listing together with the filter that produced it. */
public record CategoryListing(String filter, List<Product> products) {

  public CategoryListing {
    products = List.copyOf(products);
  }
}
