package com.tractorstore.order.infrastructure.persistence.repository;

import com.tractorstore.order.infrastructure.persistence.entity.OrderEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, UUID> {

  /** The order with its lines in one statement, only if it belongs to the session. */
  @EntityGraph(attributePaths = "lines")
  Optional<OrderEntity> findByIdAndSessionId(UUID id, UUID sessionId);
}
