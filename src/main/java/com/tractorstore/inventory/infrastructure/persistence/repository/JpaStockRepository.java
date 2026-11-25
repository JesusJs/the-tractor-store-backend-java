package com.tractorstore.inventory.infrastructure.persistence.repository;

import com.tractorstore.inventory.infrastructure.persistence.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JpaStockRepository extends JpaRepository<StockEntity, String> {

  /**
   * Conditional UPDATE: the row lock makes concurrent decreases wait and re-check the condition, so
   * stock never goes negative. No own transaction: it joins the use case one.
   *
   * @return rows updated (0 or 1)
   */
  @Modifying
  @Query(
      "update StockEntity s set s.stock = s.stock - :quantity"
          + " where s.sku = :sku and s.stock >= :quantity")
  int decreaseIfAvailable(String sku, int quantity);
}
