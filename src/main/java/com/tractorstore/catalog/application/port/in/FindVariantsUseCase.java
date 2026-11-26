package com.tractorstore.catalog.application.port.in;

import com.tractorstore.catalog.api.VariantView;
import java.util.Collection;
import java.util.List;

/** Inbound port: variants by SKU with their real price. */
public interface FindVariantsUseCase {

  /** One view per requested SKU that exists; unknown SKUs are omitted. */
  List<VariantView> find(Collection<String> skus);
}
