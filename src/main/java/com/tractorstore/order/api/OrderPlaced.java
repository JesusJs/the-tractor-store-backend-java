package com.tractorstore.order.api;

import java.util.List;
import java.util.UUID;

/**
 * An order was confirmed. Published once, inside the order's transaction, and stored in {@code
 * event_publication} until its listeners complete; new fields must stay backward compatible because
 * pending publications may be resubmitted. It carries no session id: no listener needs it and it is
 * stored in the registry (data minimization, review C5).
 */
public record OrderPlaced(UUID orderId, List<Line> lines) {

  public OrderPlaced {
    lines = List.copyOf(lines);
  }

  public record Line(String sku, int quantity) {}
}
