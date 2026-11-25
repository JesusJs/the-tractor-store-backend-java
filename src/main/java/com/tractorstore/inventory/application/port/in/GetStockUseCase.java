package com.tractorstore.inventory.application.port.in;

import com.tractorstore.inventory.domain.model.StockItem;

/** Inbound port: stock of a variant. */
public interface GetStockUseCase {

  /**
   * @throws com.tractorstore.inventory.domain.exception.StockNotFoundException if the SKU has no
   *     stock row
   */
  StockItem get(String sku);
}
