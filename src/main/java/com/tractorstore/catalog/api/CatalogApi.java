package com.tractorstore.catalog.api;

import java.util.Collection;
import java.util.List;

/** What other modules may ask the catalog (contracts/module-apis.md). Read only. */
public interface CatalogApi {

  /**
   * Variants whose SKU is in {@code skus}, with the real variant price (not the "from" price).
   * Unknown SKUs are simply absent from the result; order of the result is unspecified.
   */
  List<VariantView> findVariants(Collection<String> skus);

  /** Whether {@code storeId} is exactly the id of a catalog store. */
  boolean storeExists(String storeId);
}
