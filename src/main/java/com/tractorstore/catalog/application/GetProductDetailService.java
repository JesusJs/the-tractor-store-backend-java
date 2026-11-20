package com.tractorstore.catalog.application;

import com.tractorstore.catalog.domain.Product;
import com.tractorstore.catalog.domain.ProductNotFoundException;
import com.tractorstore.catalog.domain.port.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetProductDetailService implements GetProductDetail {

  private final ProductRepository products;

  GetProductDetailService(ProductRepository products) {
    this.products = products;
  }

  @Override
  public Product get(String id) {
    return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
  }
}
