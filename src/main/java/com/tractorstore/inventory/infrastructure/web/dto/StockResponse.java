package com.tractorstore.inventory.infrastructure.web.dto;

/** Stock of a variant ({@code InventoryStatus} in the front, {@code catalog.models.ts}). */
public record StockResponse(String sku, int stock) {}
