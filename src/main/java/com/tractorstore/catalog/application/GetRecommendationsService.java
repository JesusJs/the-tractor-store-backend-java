package com.tractorstore.catalog.application;

import com.tractorstore.catalog.domain.ColorRecommendationPolicy;
import com.tractorstore.catalog.domain.Recommendation;
import com.tractorstore.catalog.domain.port.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads the whole catalog in one query and lets the domain policy rank it (research R6). */
@Service
@Transactional(readOnly = true)
class GetRecommendationsService implements GetRecommendations {

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
