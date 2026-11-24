package com.tractorstore.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Read-only row of {@code catalog_store}; never leaves this package. */
@Entity
@Immutable
@Table(name = "catalog_store")
public class StoreEntity {

  @Id private String id;

  private String name;

  private String address;

  private String city;

  private String image;

  private int position;

  protected StoreEntity() {}

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public String getCity() {
    return city;
  }

  public String getImage() {
    return image;
  }

  public int getPosition() {
    return position;
  }
}
