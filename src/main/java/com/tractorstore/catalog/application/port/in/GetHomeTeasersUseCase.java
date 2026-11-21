package com.tractorstore.catalog.application.port.in;

import com.tractorstore.catalog.domain.model.Category;
import java.util.List;

/** Inbound port: one home teaser per category. */
public interface GetHomeTeasersUseCase {

  /** Categories in dataset order. */
  List<Category> get();
}
