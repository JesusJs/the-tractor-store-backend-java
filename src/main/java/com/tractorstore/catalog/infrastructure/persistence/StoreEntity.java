package com.tractorstore.catalog.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Read-only row of {@code catalog_store}; never leaves this package. */
@Entity
@Immutable
@Table(name = "catalog_store")
class StoreEntity {

  @Id private String id;

  private String name;

  private String address;

  private String city;

  private String image;

  private int position;

  protected StoreEntity() {}

  String getId() {
    return id;
  }

  String getName() {
    return name;
  }

  String getAddress() {
    return address;
  }

  String getCity() {
    return city;
  }

  String getImage() {
    return image;
  }

  int getPosition() {
    return position;
  }
}
