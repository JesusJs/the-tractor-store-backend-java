package com.tractorstore.order.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Order line sent by the front. Its other fields are never bound: price and names always come from
 * the catalog (research R6).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
    description =
        "Order line. productId, productName, variantName, price and image may be sent but are"
            + " ignored; taken from the catalog.")
public record OrderItemRequest(
    @Schema(description = "Variant SKU", example = "AU-07-YE") @NotBlank @Size(max = 20) String variantId,
    @Schema(example = "1") @NotNull @Min(1) @Max(99) Integer quantity) {}
