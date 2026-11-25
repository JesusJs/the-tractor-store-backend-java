package com.tractorstore.inventory.application.service;

import com.tractorstore.inventory.api.InventoryApi;
import com.tractorstore.inventory.application.port.in.DecreaseStockUseCase;
import org.springframework.stereotype.Service;

/** Public face of the module: delegates to its use cases, adds no logic. */
@Service
class InventoryApiAdapter implements InventoryApi {

  private final DecreaseStockUseCase decreaseStock;

  InventoryApiAdapter(DecreaseStockUseCase decreaseStock) {
    this.decreaseStock = decreaseStock;
  }

  @Override
  public boolean decreaseStock(String sku, int quantity) {
    return decreaseStock.decrease(sku, quantity);
  }
}
