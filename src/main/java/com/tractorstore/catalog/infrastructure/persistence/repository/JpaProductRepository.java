package com.tractorstore.catalog.infrastructure.persistence.repository;

import com.tractorstore.catalog.infrastructure.persistence.entity.ProductEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Every read fetches category, variants and highlights in a single SQL statement (FR-024). */
public interface JpaProductRepository extends JpaRepository<ProductEntity, String> {

  @EntityGraph(attributePaths = {"category", "variants", "highlights"})
  List<ProductEntity> findAllByOrderByPositionAsc();

  @EntityGraph(attributePaths = {"category", "variants", "highlights"})
  List<ProductEntity> findAllByCategoryCodeOrderByPositionAsc(String categoryCode);

  @EntityGraph(attributePaths = {"category", "variants", "highlights"})
  Optional<ProductEntity> findWithDetailsById(String id);

  /**
   * Products containing any of {@code skus} with all their variants: the filter lives in a subquery
   * so the fetch join is not narrowed to the matching variants (research R8).
   */
  @EntityGraph(attributePaths = {"category", "variants", "highlights"})
  @Query(
      "select p from ProductEntity p where exists "
          + "(select v from p.variants v where v.sku in :skus)")
  List<ProductEntity> findAllContainingSkus(Collection<String> skus);
}
