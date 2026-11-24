package com.tractorstore.catalog.application.service;

import com.tractorstore.catalog.application.port.in.GetProductDetailUseCase;
import com.tractorstore.catalog.domain.exception.ProductNotFoundException;
import com.tractorstore.catalog.domain.model.Product;
import com.tractorstore.catalog.domain.port.out.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetProductDetailService implements GetProductDetailUseCase {

  private final ProductRepository products;

  GetProductDetailService(ProductRepository products) {
    this.products = products;
  }

  @Override
  public Product get(String id) {
    return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
  }
}
