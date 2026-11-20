package com.tractorstore.catalog.application;

import com.tractorstore.catalog.domain.Category;
import java.util.List;

/** Inbound port: one home teaser per category. */
public interface GetHomeTeasers {

  /** Categories in dataset order. */
  List<Category> get();
}
