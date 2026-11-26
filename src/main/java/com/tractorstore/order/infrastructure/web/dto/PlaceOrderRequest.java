package com.tractorstore.order.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tractorstore.shared.infrastructure.web.InputPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Checkout body of the front. {@code items} has no {@code @NotEmpty}: an empty cart is a 409
 * CART_EMPTY decided by the use case, not a 400 (research R6). Ids only take letters, digits and
 * hyphens and free texts no control or invisible format characters (review PR #5 A07): PostgreSQL
 * rejects {@code \u0000} with a 500 (review #4).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceOrderRequest(
    @Schema(example = "Ada")
        @NotBlank @Size(max = 100) @Pattern(regexp = TEXT, message = TEXT_MESSAGE) String firstName,
    @Schema(example = "Lovelace")
        @NotBlank @Size(max = 100) @Pattern(regexp = TEXT, message = TEXT_MESSAGE) String lastName,
    @Schema(description = "Id of a catalog store", example = "store-a")
        @NotBlank @Size(max = 20) @Pattern(regexp = ID, message = ID_MESSAGE) String storeId,
    @Schema(
            description = "Optional stores separated by \", \"",
            example = "Big Micro Machines, Central Mall")
        @Size(max = 500) @Pattern(regexp = TEXT, message = TEXT_MESSAGE) String extraPickups,
    @Size(max = 50) List<@NotNull @Valid OrderItemRequest> items) {

  /** Catalog ids (SKU, store); blank is left to {@code @NotBlank}. */
  static final String ID = InputPatterns.CATALOG_ID;

  static final String ID_MESSAGE = InputPatterns.CATALOG_ID_MESSAGE;

  /**
   * Any text (accents and other scripts included) without control (Cc) or invisible format (Cf)
   * characters nor line or paragraph separators (Zl, Zp): U+202E (right-to-left override) would
   * spoof how the receipt and the logs read (review PR #5 A07).
   */
  static final String TEXT = "[^\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]*";

  static final String TEXT_MESSAGE = "must not contain control or invisible format characters";
}
