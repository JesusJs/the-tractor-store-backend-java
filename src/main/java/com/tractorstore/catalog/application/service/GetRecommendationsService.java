package com.tractorstore.catalog.application.service;

import com.tractorstore.catalog.application.port.in.GetRecommendationsUseCase;
import com.tractorstore.catalog.domain.model.Recommendation;
import com.tractorstore.catalog.domain.port.out.ProductRepository;
import com.tractorstore.catalog.domain.service.ColorRecommendationPolicy;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads the whole catalog in one query and lets the domain policy rank it (research R6). */
@Service
@Transactional(readOnly = true)
class GetRecommendationsService implements GetRecommendationsUseCase {

  private final ProductRepository products;
  private final ColorRecommendationPolicy policy = new ColorRecommendationPolicy();

  GetRecommendationsService(ProductRepository products) {
    this.products = products;
  }

  @Override
  public List<Recommendation> get(List<String> skus) {
    return policy.recommend(products.findAll(), skus);
  }
}
