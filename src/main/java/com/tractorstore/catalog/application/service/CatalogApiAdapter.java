package com.tractorstore.catalog.application.service;

import com.tractorstore.catalog.api.CatalogApi;
import com.tractorstore.catalog.api.VariantView;
import com.tractorstore.catalog.application.port.in.FindVariantsUseCase;
import com.tractorstore.catalog.application.port.in.StoreExistsUseCase;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

/** Public face of the module: delegates to its use cases, adds no logic. */
@Service
class CatalogApiAdapter implements CatalogApi {

  private final FindVariantsUseCase findVariants;
  private final StoreExistsUseCase storeExists;

  CatalogApiAdapter(FindVariantsUseCase findVariants, StoreExistsUseCase storeExists) {
    this.findVariants = findVariants;
    this.storeExists = storeExists;
  }

  @Override
  public List<VariantView> findVariants(Collection<String> skus) {
    return findVariants.find(skus);
  }

  @Override
  public boolean storeExists(String storeId) {
    return storeExists.exists(storeId);
  }
}
