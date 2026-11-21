package com.tractorstore.catalog.application.port.in;

import com.tractorstore.catalog.domain.model.Product;

/** Inbound port: one product with its variants and highlights. */
public interface GetProductDetailUseCase {

  /**
   * @throws com.tractorstore.catalog.domain.exception.ProductNotFoundException if no product has
   *     that id
   */
  Product get(String id);
}
