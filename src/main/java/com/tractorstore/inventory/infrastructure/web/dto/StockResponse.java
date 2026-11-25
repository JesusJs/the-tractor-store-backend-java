package com.tractorstore.inventory.infrastructure.web.dto;

/** Stock of a variant ({@code InventoryItem} in the front). */
public record StockResponse(String sku, int stock) {}
