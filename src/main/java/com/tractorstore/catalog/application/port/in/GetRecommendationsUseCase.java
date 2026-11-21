package com.tractorstore.catalog.application.port.in;

import com.tractorstore.catalog.domain.model.Recommendation;
import java.util.List;

/** Inbound port: up to four products close in color to the given SKUs; never a business error. */
public interface GetRecommendationsUseCase {

  /**
   * @param skus SKUs to start from; may be {@code null} or contain unknown SKUs (default list)
   */
  List<Recommendation> get(List<String> skus);
}
