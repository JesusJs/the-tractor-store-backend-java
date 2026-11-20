package com.tractorstore.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Every read fetches category, variants and highlights in a single SQL statement (FR-024). */
interface JpaProductRepository extends JpaRepository<ProductEntity, String> {

  @EntityGraph(attributePaths = {"category", "variants", "highlights"})
  List<ProductEntity> findAllByOrderByPositionAsc();

  @EntityGraph(attributePaths = {"category", "variants", "highlights"})
  List<ProductEntity> findAllByCategoryCodeOrderByPositionAsc(String categoryCode);

  @EntityGraph(attributePaths = {"category", "variants", "highlights"})
  Optional<ProductEntity> findWithDetailsById(String id);
}
