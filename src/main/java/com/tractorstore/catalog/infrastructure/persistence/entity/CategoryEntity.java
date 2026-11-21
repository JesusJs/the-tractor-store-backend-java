package com.tractorstore.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Read-only row of {@code catalog_category}; never leaves this package. */
@Entity
@Immutable
@Table(name = "catalog_category")
public class CategoryEntity {

  @Id private String code;

  @Column(name = "brand_label")
  private String brandLabel;

  @Column(name = "teaser_title")
  private String teaserTitle;

  @Column(name = "teaser_image")
  private String teaserImage;

  private int position;

  protected CategoryEntity() {}

  public String getCode() {
    return code;
  }

  public String getBrandLabel() {
    return brandLabel;
  }

  public String getTeaserTitle() {
    return teaserTitle;
  }

  public String getTeaserImage() {
    return teaserImage;
  }

  public int getPosition() {
    return position;
  }
}
