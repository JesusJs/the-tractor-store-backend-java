package com.tractorstore.order.domain.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Confirmed order of an anonymous session. Immutable; amounts are computed once in {@link #place}.
 */
public final class Order {

  private static final String PICKUP_SEPARATOR = ", ";

  private final OrderId id;
  private final UUID sessionId;
  private final String firstName;
  private final String lastName;
  private final String storeId;
  private final String extraPickups;
  private final List<OrderLine> lines;
  private final Money subTotal;
  private final Money tax;
  private final Money total;
  private final OrderStatus status;
  private final Instant placedAt;

  private Order(
      OrderId id,
      UUID sessionId,
      String firstName,
      String lastName,
      String storeId,
      String extraPickups,
      List<OrderLine> lines,
      Money subTotal,
      Money tax,
      Money total,
      OrderStatus status,
      Instant placedAt) {
    this.id = requireValue(id, "id");
    this.sessionId = requireValue(sessionId, "sessionId");
    this.firstName = requireText(firstName, "firstName");
    this.lastName = requireText(lastName, "lastName");
    this.storeId = requireText(storeId, "storeId");
    this.extraPickups = extraPickups;
    this.lines = List.copyOf(requireValue(lines, "lines"));
    if (this.lines.isEmpty()) {
      throw new IllegalArgumentException("an order needs at least one line");
    }
    this.subTotal = requireValue(subTotal, "subTotal");
    this.tax = requireValue(tax, "tax");
    this.total = requireValue(total, "total");
    this.status = requireValue(status, "status");
    this.placedAt = requireValue(placedAt, "placedAt");
  }

  /** New order: {@code subTotal = Σ lineTotal}, {@code tax = 21 %}, {@code total = sum}. */
  public static Order place(
      OrderId id,
      UUID sessionId,
      String firstName,
      String lastName,
      String storeId,
      String extraPickups,
      List<OrderLine> lines,
      Instant placedAt) {
    requireValue(lines, "lines");
    Money subTotal = lines.stream().map(OrderLine::lineTotal).reduce(Money.ZERO, Money::plus);
    Money tax = Vat.taxOn(subTotal);
    return new Order(
        id,
        sessionId,
        firstName,
        lastName,
        storeId,
        extraPickups,
        lines,
        subTotal,
        tax,
        subTotal.plus(tax),
        OrderStatus.PLACED,
        placedAt);
  }

  /** Order read back from persistence, with its stored amounts (never recomputed). */
  public static Order restore(
      OrderId id,
      UUID sessionId,
      String firstName,
      String lastName,
      String storeId,
      String extraPickups,
      List<OrderLine> lines,
      Money subTotal,
      Money tax,
      Money total,
      OrderStatus status,
      Instant placedAt) {
    return new Order(
        id,
        sessionId,
        firstName,
        lastName,
        storeId,
        extraPickups,
        lines,
        subTotal,
        tax,
        total,
        status,
        placedAt);
  }

  /** Extra pickups split by the literal {@code ", "} without empty fragments; never null. */
  public List<String> extraPickupList() {
    if (extraPickups == null || extraPickups.isBlank()) {
      return List.of();
    }
    return Arrays.stream(extraPickups.split(PICKUP_SEPARATOR))
        .filter(fragment -> !fragment.isBlank())
        .toList();
  }

  public boolean belongsTo(UUID session) {
    return sessionId.equals(session);
  }

  public OrderId id() {
    return id;
  }

  public UUID sessionId() {
    return sessionId;
  }

  public String firstName() {
    return firstName;
  }

  public String lastName() {
    return lastName;
  }

  public String storeId() {
    return storeId;
  }

  /** As received; may be null. */
  public String extraPickups() {
    return extraPickups;
  }

  public List<OrderLine> lines() {
    return lines;
  }

  public Money subTotal() {
    return subTotal;
  }

  public Money tax() {
    return tax;
  }

  public Money total() {
    return total;
  }

  public OrderStatus status() {
    return status;
  }

  public Instant placedAt() {
    return placedAt;
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  private static <T> T requireValue(T value, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    return value;
  }
}
