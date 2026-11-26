package com.tractorstore.order.domain.model;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Order identifier: a random UUID, printed in lowercase. */
public record OrderId(UUID value) {

  /** Same check as the session cookie: {@code UUID.fromString} also accepts {@code 1-1-1-1-1}. */
  private static final Pattern CANONICAL_UUID =
      Pattern.compile("^[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$");

  public OrderId {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }
  }

  public static OrderId newId() {
    return new OrderId(UUID.randomUUID());
  }

  /** The id when {@code raw} is a canonical UUID; empty for anything else. */
  public static Optional<OrderId> parse(String raw) {
    if (raw == null || !CANONICAL_UUID.matcher(raw).matches()) {
      return Optional.empty();
    }
    return Optional.of(new OrderId(UUID.fromString(raw)));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
