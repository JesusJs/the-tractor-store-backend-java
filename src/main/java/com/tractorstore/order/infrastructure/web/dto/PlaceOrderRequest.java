package com.tractorstore.order.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Checkout body of the front. {@code items} has no {@code @NotEmpty}: an empty cart is a 409
 * CART_EMPTY decided by the use case, not a 400 (research R6).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceOrderRequest(
    @Schema(example = "Ada") @NotBlank @Size(max = 100) String firstName,
    @Schema(example = "Lovelace") @NotBlank @Size(max = 100) String lastName,
    @Schema(description = "Id of a catalog store", example = "store-a") @NotBlank @Size(max = 20) String storeId,
    @Schema(
            description = "Optional stores separated by \", \"",
            example = "Big Micro Machines, Central Mall")
        @Size(max = 500) String extraPickups,
    @Size(max = 50) List<@NotNull @Valid OrderItemRequest> items) {}
