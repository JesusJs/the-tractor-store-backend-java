package com.tractorstore.order.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Receipt line. {@code variantName} is the SKU, as the front expects (FR-009). */
public record OrderReceiptItemResponse(
    String productId,
    String variantId,
    String productName,
    @Schema(description = "Same value as variantId (SKU)") String variantName,
    @Schema(description = "Real variant price from the catalog") BigDecimal price,
    int quantity,
    String image) {}
