package com.tractorstore.catalog.application;

import com.tractorstore.catalog.domain.Product;

/** Inbound port: one product with its variants and highlights. */
public interface GetProductDetail {

  /**
   * @throws com.tractorstore.catalog.domain.ProductNotFoundException if no product has that id
   */
  Product get(String id);
}
