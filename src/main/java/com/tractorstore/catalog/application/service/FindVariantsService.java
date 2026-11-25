package com.tractorstore.catalog.application.service;

import com.tractorstore.catalog.api.VariantView;
import com.tractorstore.catalog.application.port.in.FindVariantsUseCase;
import com.tractorstore.catalog.domain.port.out.ProductRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One SQL statement whatever the number of SKUs (research R8); joins the caller's transaction. */
@Service
@Transactional(readOnly = true)
class FindVariantsService implements FindVariantsUseCase {

  private final ProductRepository products;

  FindVariantsService(ProductRepository products) {
    this.products = products;
  }

  @Override
  public List<VariantView> find(Collection<String> skus) {
    Set<String> requested = Set.copyOf(skus);
    return products.findByVariantSkus(skus).stream()
        .flatMap(
            product ->
                product.variants().stream()
                    .filter(variant -> requested.contains(variant.sku()))
                    .map(
                        variant ->
                            new VariantView(
                                variant.sku(),
                                product.id(),
                                product.name(),
                                variant.name(),
                                variant.price(),
                                variant.image())))
        .toList();
  }
}
