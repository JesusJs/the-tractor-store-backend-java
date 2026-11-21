package com.tractorstore.catalog.infrastructure.web.controller;

import com.tractorstore.catalog.application.port.in.GetCategoryListingUseCase;
import com.tractorstore.catalog.application.port.in.GetHomeTeasersUseCase;
import com.tractorstore.catalog.application.port.in.GetProductDetailUseCase;
import com.tractorstore.catalog.application.port.in.GetRecommendationsUseCase;
import com.tractorstore.catalog.application.port.in.ListStoresUseCase;
import com.tractorstore.catalog.infrastructure.web.dto.CategoryResponse;
import com.tractorstore.catalog.infrastructure.web.dto.ProductDetailResponse;
import com.tractorstore.catalog.infrastructure.web.dto.ProductItemResponse;
import com.tractorstore.catalog.infrastructure.web.dto.StoreResponse;
import com.tractorstore.catalog.infrastructure.web.dto.TeaserResponse;
import com.tractorstore.catalog.infrastructure.web.mapper.CatalogDtoMapper;
import com.tractorstore.shared.infrastructure.web.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only catalog endpoints with the exact shape of the front (ADR 0008). No class-level
 * {@code @Validated}: parameter constraints use Spring MVC built-in method validation, which raises
 * {@code HandlerMethodValidationException} (400 VALIDATION_ERROR, research R8).
 */
@RestController
@RequestMapping(CatalogPaths.BASE)
@Tag(name = "Catalog", description = "Tractors, categories, recommendations and stores")
class CatalogController {

  private final GetHomeTeasersUseCase getHomeTeasers;
  private final GetCategoryListingUseCase getCategoryListing;
  private final GetProductDetailUseCase getProductDetail;
  private final GetRecommendationsUseCase getRecommendations;
  private final ListStoresUseCase listStores;

  CatalogController(
      GetHomeTeasersUseCase getHomeTeasers,
      GetCategoryListingUseCase getCategoryListing,
      GetProductDetailUseCase getProductDetail,
      GetRecommendationsUseCase getRecommendations,
      ListStoresUseCase listStores) {
    this.getHomeTeasers = getHomeTeasers;
    this.getCategoryListing = getCategoryListing;
    this.getProductDetail = getProductDetail;
    this.getRecommendations = getRecommendations;
    this.listStores = listStores;
  }

  @GetMapping("/home")
  @Operation(
      summary = "Home teasers",
      description = "One teaser per category (classic first) for the home page.")
  @ApiResponse(responseCode = "200", description = "Teasers")
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected error",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  List<TeaserResponse> home() {
    return CatalogDtoMapper.teasers(getHomeTeasers.get());
  }

  @GetMapping("/categories/{filter}")
  @Operation(
      summary = "Category listing",
      description =
          "Products of a category in dataset order. The filter is exactly all, classic or"
              + " autonomous (case sensitive); price is the lowest variant price.")
  @ApiResponse(responseCode = "200", description = "Listing")
  @ApiResponse(
      responseCode = "400",
      description = "Filter longer than 20 characters (VALIDATION_ERROR)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Unknown filter (CATEGORY_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected error",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  CategoryResponse category(@PathVariable @Size(max = 20) String filter) {
    return CatalogDtoMapper.category(getCategoryListing.get(filter));
  }

  @GetMapping("/products/{id}")
  @Operation(
      summary = "Product detail",
      description =
          "One product with its SKUs and highlights in dataset order; price is the lowest variant"
              + " price.")
  @ApiResponse(responseCode = "200", description = "Product")
  @ApiResponse(
      responseCode = "400",
      description = "Id longer than 20 characters (VALIDATION_ERROR)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Unknown product (PRODUCT_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected error",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  ProductDetailResponse product(@PathVariable @Size(max = 20) String id) {
    return CatalogDtoMapper.detail(getProductDetail.get(id));
  }

  @GetMapping("/recommendations")
  @Operation(
      summary = "Recommendations by color",
      description =
          "Up to 4 products closest in RGB to the average color of the given SKUs; variants[0] is"
              + " the recommended SKU. Without known SKUs it returns the default list. Never"
              + " returns business errors.")
  @ApiResponse(responseCode = "200", description = "Recommended products")
  @ApiResponse(
      responseCode = "400",
      description = "More than 50 SKUs or a SKU longer than 20 characters (VALIDATION_ERROR)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected error",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  List<ProductItemResponse> recommendations(
      @RequestParam(required = false) @Size(max = 50) List<@Size(max = 20) String> skus) {
    return CatalogDtoMapper.recommendations(getRecommendations.get(skus));
  }

  @GetMapping("/stores")
  @Operation(summary = "Stores", description = "Physical pick-up stores in dataset order.")
  @ApiResponse(responseCode = "200", description = "Stores")
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected error",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  List<StoreResponse> stores() {
    return CatalogDtoMapper.stores(listStores.list());
  }
}
