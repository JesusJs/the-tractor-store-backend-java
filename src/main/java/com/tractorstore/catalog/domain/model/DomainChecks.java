package com.tractorstore.catalog.domain.model;

/** Invariant checks shared by the catalog domain types. */
final class DomainChecks {

  private DomainChecks() {}

  static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  static <T> T requireValue(T value, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    return value;
  }
}
