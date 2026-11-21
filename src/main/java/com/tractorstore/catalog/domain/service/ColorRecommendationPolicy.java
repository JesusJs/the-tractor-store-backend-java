package com.tractorstore.catalog.domain.service;

import com.tractorstore.catalog.domain.model.Color;
import com.tractorstore.catalog.domain.model.Product;
import com.tractorstore.catalog.domain.model.Recommendation;
import com.tractorstore.catalog.domain.model.Variant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Recommends products by RGB distance to the average color of the requested SKUs (FR-010 – FR-013,
 * research R6). The category plays no part, and it never throws business errors: with no known SKU
 * it returns the default list (first variant of the first products).
 */
public final class ColorRecommendationPolicy {

  public static final int LIMIT = 4;

  private record Candidate(Product product, Variant variant, double distance) {}

  /**
   * @param catalog every product in dataset order
   * @param requestedSkus SKUs to start from; may be {@code null}, contain blanks, repeats or
   *     unknown SKUs
   */
  public List<Recommendation> recommend(List<Product> catalog, Collection<String> requestedSkus) {
    Map<String, Variant> variantsBySku = new HashMap<>();
    catalog.forEach(product -> product.variants().forEach(v -> variantsBySku.put(v.sku(), v)));

    Set<String> requested = knownSkus(requestedSkus, variantsBySku.keySet());
    if (requested.isEmpty()) {
      return defaultList(catalog);
    }

    Color target =
        Color.average(requested.stream().map(sku -> variantsBySku.get(sku).color()).toList());
    Map<String, Recommendation> closestByProduct = new LinkedHashMap<>();
    catalog.stream()
        .flatMap(
            product ->
                product.variants().stream()
                    .filter(variant -> !requested.contains(variant.sku()))
                    .map(
                        variant ->
                            new Candidate(product, variant, variant.color().distanceTo(target))))
        .sorted(
            Comparator.comparingDouble(Candidate::distance)
                .thenComparing(candidate -> candidate.variant().sku()))
        .forEachOrdered(
            candidate ->
                closestByProduct.putIfAbsent(
                    candidate.product().id(),
                    new Recommendation(candidate.product(), candidate.variant())));
    return closestByProduct.values().stream().limit(LIMIT).toList();
  }

  /** Trimmed, non-blank, de-duplicated SKUs that exist in the catalog. */
  private static Set<String> knownSkus(Collection<String> requestedSkus, Set<String> catalogSkus) {
    Set<String> known = new LinkedHashSet<>();
    if (requestedSkus != null) {
      requestedSkus.stream()
          .filter(Objects::nonNull)
          .map(String::trim)
          .filter(catalogSkus::contains)
          .forEach(known::add);
    }
    return known;
  }

  private static List<Recommendation> defaultList(List<Product> catalog) {
    return catalog.stream()
        .limit(LIMIT)
        .map(product -> new Recommendation(product, product.variants().getFirst()))
        .toList();
  }
}
