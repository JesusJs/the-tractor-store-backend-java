package com.tractorstore.inventory.application.service;

import com.tractorstore.inventory.application.port.in.GetStockUseCase;
import com.tractorstore.inventory.domain.exception.StockNotFoundException;
import com.tractorstore.inventory.domain.model.StockItem;
import com.tractorstore.inventory.domain.port.out.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetStockService implements GetStockUseCase {

  private final StockRepository stock;

  GetStockService(StockRepository stock) {
    this.stock = stock;
  }

  @Override
  public StockItem get(String sku) {
    return stock.findBySku(sku).orElseThrow(() -> new StockNotFoundException(sku));
  }
}
