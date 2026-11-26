package com.tractorstore.order.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Row of {@code order_line}; its {@code position} is the list index of the order. */
@Embeddable
public class OrderLineEmbeddable {

  private String sku;

  private String productId;

  private String productName;

  private String variantName;

  @Column(precision = 10, scale = 2)
  private BigDecimal unitPrice;

  private int quantity;

  private String image;

  protected OrderLineEmbeddable() {}

  public OrderLineEmbeddable(
      String sku,
      String productId,
      String productName,
      String variantName,
      BigDecimal unitPrice,
      int quantity,
      String image) {
    this.sku = sku;
    this.productId = productId;
    this.productName = productName;
    this.variantName = variantName;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.image = image;
  }

  public String getSku() {
    return sku;
  }

  public String getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public String getVariantName() {
    return variantName;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getImage() {
    return image;
  }
}
