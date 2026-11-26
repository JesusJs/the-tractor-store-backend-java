package com.tractorstore.order.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** Receipt of a confirmed order, identical in the POST and the later GET (FR-020). */
public record OrderReceiptResponse(
    String id,
    String firstName,
    String lastName,
    String storeId,
    @Schema(description = "Extra pickup stores; empty when none") List<String> extraPickups,
    List<OrderReceiptItemResponse> items,
    BigDecimal subTotal,
    @Schema(description = "21 % VAT, two decimals HALF_UP") BigDecimal tax,
    BigDecimal total,
    @Schema(description = "ISO-8601 UTC with milliseconds", example = "2026-11-24T21:15:03.123Z")
        String placedAt,
    @Schema(example = "PLACED") String status) {}
