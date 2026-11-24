package archfixtures.tableprefix.shop.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Violation: table not prefixed with its module ({@code shop_}). */
@Entity
@Table(name = "catalog_thing")
public class UnprefixedEntity {

  @Id String id;
}
