package com.tractorstore.inventory.infrastructure.web.controller;

import com.tractorstore.inventory.application.port.in.GetStockUseCase;
import com.tractorstore.inventory.domain.model.StockItem;
import com.tractorstore.inventory.infrastructure.web.dto.StockResponse;
import com.tractorstore.shared.infrastructure.web.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stock per variant. Needs no session: it neither reads nor issues {@code ts_session}. Parameter
 * constraints use Spring MVC built-in method validation (400 VALIDATION_ERROR).
 */
@RestController
@RequestMapping(InventoryPaths.BASE)
@Tag(name = "Inventory", description = "Units left of each variant")
class InventoryController {

  private final GetStockUseCase getStock;

  InventoryController(GetStockUseCase getStock) {
    this.getStock = getStock;
  }

  @GetMapping("/{sku}")
  @Operation(
      summary = "Stock of a variant",
      description =
          "Units left of the variant with this SKU; 0 means out of stock. No session cookie is"
              + " read or set.")
  @ApiResponse(responseCode = "200", description = "Stock")
  @ApiResponse(
      responseCode = "400",
      description = "SKU longer than 20 characters (VALIDATION_ERROR)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Unknown SKU (VARIANT_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Unexpected error",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  StockResponse stock(@PathVariable @Size(max = 20) String sku) {
    StockItem item = getStock.get(sku);
    return new StockResponse(item.sku(), item.units());
  }
}
