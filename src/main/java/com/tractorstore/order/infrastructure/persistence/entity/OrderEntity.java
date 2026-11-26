package com.tractorstore.order.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Row of {@code order_order} with its lines. The id is assigned by the domain, so {@link
 * Persistable} tells Spring Data it is new: an insert without a previous select.
 */
@Entity
@Table(name = "order_order")
public class OrderEntity implements Persistable<UUID> {

  @Id private UUID id;

  private UUID sessionId;

  private String firstName;

  private String lastName;

  private String storeId;

  private String extraPickups;

  @Column(precision = 10, scale = 2)
  private BigDecimal subTotal;

  @Column(precision = 10, scale = 2)
  private BigDecimal tax;

  @Column(precision = 10, scale = 2)
  private BigDecimal total;

  private String status;

  private Instant placedAt;

  @ElementCollection
  @CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_id"))
  @OrderColumn(name = "position")
  private List<OrderLineEmbeddable> lines = new ArrayList<>();

  @Transient private boolean isNew;

  protected OrderEntity() {}

  public OrderEntity(
      UUID id,
      UUID sessionId,
      String firstName,
      String lastName,
      String storeId,
      String extraPickups,
      List<OrderLineEmbeddable> lines,
      BigDecimal subTotal,
      BigDecimal tax,
      BigDecimal total,
      String status,
      Instant placedAt) {
    this.id = id;
    this.sessionId = sessionId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.storeId = storeId;
    this.extraPickups = extraPickups;
    this.lines = new ArrayList<>(lines);
    this.subTotal = subTotal;
    this.tax = tax;
    this.total = total;
    this.status = status;
    this.placedAt = placedAt;
    this.isNew = true;
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    isNew = false;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getStoreId() {
    return storeId;
  }

  public String getExtraPickups() {
    return extraPickups;
  }

  public List<OrderLineEmbeddable> getLines() {
    return lines;
  }

  public BigDecimal getSubTotal() {
    return subTotal;
  }

  public BigDecimal getTax() {
    return tax;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public String getStatus() {
    return status;
  }

  public Instant getPlacedAt() {
    return placedAt;
  }
}
