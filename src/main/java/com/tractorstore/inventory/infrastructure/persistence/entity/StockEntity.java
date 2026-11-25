package com.tractorstore.inventory.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code inventory_stock}; never leaves the persistence adapter. */
@Entity
@Table(name = "inventory_stock")
public class StockEntity {

  @Id private String sku;

  private int stock;

  protected StockEntity() {}

  public String getSku() {
    return sku;
  }

  public int getStock() {
    return stock;
  }
}
