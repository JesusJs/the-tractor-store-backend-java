package com.tractorstore.inventory.infrastructure.persistence.adapter;

import com.tractorstore.inventory.domain.model.StockItem;
import com.tractorstore.inventory.domain.port.out.StockRepository;
import com.tractorstore.inventory.infrastructure.persistence.repository.JpaStockRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class StockRepositoryAdapter implements StockRepository {

  private final JpaStockRepository stock;

  StockRepositoryAdapter(JpaStockRepository stock) {
    this.stock = stock;
  }

  @Override
  public Optional<StockItem> findBySku(String sku) {
    return stock.findById(sku).map(entity -> new StockItem(entity.getSku(), entity.getStock()));
  }

  @Override
  public boolean decreaseIfAvailable(String sku, int quantity) {
    return stock.decreaseIfAvailable(sku, quantity) == 1;
  }
}
