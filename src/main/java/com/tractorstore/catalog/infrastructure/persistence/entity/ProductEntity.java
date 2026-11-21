package com.tractorstore.catalog.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Immutable;

/**
 * Read-only row of {@code catalog_product}. Both collections are indexed lists ({@link
 * OrderColumn}), not bags, so one query can fetch them together in dataset order (research R5).
 */
@Entity
@Immutable
@Table(name = "catalog_product")
public class ProductEntity {

  @Id private String id;

  private String name;

  private int position;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_code")
  private CategoryEntity category;

  @ElementCollection
  @CollectionTable(
      name = "catalog_product_highlight",
      joinColumns = @JoinColumn(name = "product_id"))
  @OrderColumn(name = "position")
  @Column(name = "text")
  private List<String> highlights = new ArrayList<>();

  @OneToMany
  @JoinColumn(name = "product_id")
  @OrderColumn(name = "position")
  private List<VariantEntity> variants = new ArrayList<>();

  protected ProductEntity() {}

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getPosition() {
    return position;
  }

  public CategoryEntity getCategory() {
    return category;
  }

  public List<String> getHighlights() {
    return highlights;
  }

  public List<VariantEntity> getVariants() {
    return variants;
  }
}
