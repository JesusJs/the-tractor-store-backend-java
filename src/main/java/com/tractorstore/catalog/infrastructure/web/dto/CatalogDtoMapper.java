package com.tractorstore.catalog.infrastructure.web.dto;

import com.tractorstore.catalog.application.CategoryListing;
import com.tractorstore.catalog.domain.Category;
import com.tractorstore.catalog.domain.Product;
import com.tractorstore.catalog.domain.Recommendation;
import com.tractorstore.catalog.domain.Store;
import java.util.List;

/** Domain → HTTP mapping (data-model §6). No DTO carries stock, engine power or colors. */
public final class CatalogDtoMapper {

  /** Filters the front offers, always the same three (FR-005). */
  static final List<String> AVAILABLE_FILTERS = List.of("all", "classic", "autonomous");

  private CatalogDtoMapper() {}

  public static List<TeaserResponse> teasers(List<Category> categories) {
    return categories.stream().map(CatalogDtoMapper::teaser).toList();
  }

  public static CategoryResponse category(CategoryListing listing) {
    return new CategoryResponse(
        listing.filter(),
        listing.products().stream().map(CatalogDtoMapper::item).toList(),
        AVAILABLE_FILTERS);
  }

  public static ProductDetailResponse detail(Product product) {
    return new ProductDetailResponse(
        product.id(),
        product.name(),
        product.brand(),
        product.startPrice(),
        product.mainImage(),
        product.description(),
        product.skus(),
        product.highlights());
  }

  /** Same shape as a listing item, with the recommended SKU first (the front reads variants[0]). */
  public static List<ProductItemResponse> recommendations(List<Recommendation> recommendations) {
    return recommendations.stream()
        .map(r -> item(r.product(), r.product().skusStartingWith(r.variant().sku())))
        .toList();
  }

  public static List<StoreResponse> stores(List<Store> stores) {
    return stores.stream().map(CatalogDtoMapper::store).toList();
  }

  private static TeaserResponse teaser(Category category) {
    return new TeaserResponse(
        category.code(), category.teaserTitle(), category.teaserImage(), category.code());
  }

  private static ProductItemResponse item(Product product) {
    return item(product, product.skus());
  }

  private static ProductItemResponse item(Product product, List<String> variants) {
    return new ProductItemResponse(
        product.id(),
        product.name(),
        product.brand(),
        product.startPrice(),
        product.mainImage(),
        variants,
        product.description());
  }

  private static StoreResponse store(Store store) {
    return new StoreResponse(
        store.id(), store.name(), store.address(), store.city(), store.image());
  }
}
