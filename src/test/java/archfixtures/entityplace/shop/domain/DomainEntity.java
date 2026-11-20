package archfixtures.entityplace.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Violation: JPA entity outside infrastructure.persistence. */
@Entity
@Table(name = "shop_thing")
public class DomainEntity {

  @Id String id;
}
