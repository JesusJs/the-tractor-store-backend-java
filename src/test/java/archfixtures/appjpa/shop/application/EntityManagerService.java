package archfixtures.appjpa.shop.application;

import jakarta.persistence.EntityManager;

/** Violation: a use case that talks to JPA directly instead of through an outbound port. */
public class EntityManagerService {

  EntityManager entityManager;
}
