package com.tractorstore.inventory.application.service;

import com.tractorstore.inventory.application.port.in.DecreaseStockUseCase;
import com.tractorstore.inventory.domain.port.out.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Only runs inside the caller's transaction (MANDATORY), so the order rolls the decrease back on
 * failure; without one it fails instead of committing the decrease on its own.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class DecreaseStockService implements DecreaseStockUseCase {

  private final StockRepository stock;

  DecreaseStockService(StockRepository stock) {
    this.stock = stock;
  }

  @Override
  public boolean decrease(String sku, int quantity) {
    if (quantity < 1) {
      throw new IllegalArgumentException("quantity must be at least 1: " + quantity);
    }
    return stock.decreaseIfAvailable(sku, quantity);
  }
}
