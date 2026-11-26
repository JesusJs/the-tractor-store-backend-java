package com.tractorstore.shared.infrastructure.web;

/**
 * Bean Validation patterns shared by the request adapters of several modules, so a SKU is checked
 * the same way in a path ({@code GET /inventory/{sku}}) and in a body ({@code POST /orders})
 * (review PR #5 A06).
 */
public final class InputPatterns {

  /** Catalog ids (SKU, store): letters, digits and hyphens; blank is left to {@code @NotBlank}. */
  public static final String CATALOG_ID = "[A-Za-z0-9-]*";

  public static final String CATALOG_ID_MESSAGE = "must only contain letters, digits and hyphens";

  private InputPatterns() {}
}
