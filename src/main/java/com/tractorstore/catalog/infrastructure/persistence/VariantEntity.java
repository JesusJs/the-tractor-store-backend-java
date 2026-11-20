package com.tractorstore.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

/** Read-only row of {@code catalog_variant}; owned by its product (no back reference). */
@Entity
@Immutable
@Table(name = "catalog_variant")
class VariantEntity {

  @Id private String sku;

  private String name;

  private String color;

  @Column(precision = 10, scale = 2)
  private BigDecimal price;

  private String image;

  protected VariantEntity() {}

  String getSku() {
    return sku;
  }

  String getName() {
    return name;
  }

  String getColor() {
    return color;
  }

  BigDecimal getPrice() {
    return price;
  }

  String getImage() {
    return image;
  }
}
