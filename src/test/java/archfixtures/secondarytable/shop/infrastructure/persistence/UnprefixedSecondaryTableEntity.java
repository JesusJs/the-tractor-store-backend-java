package archfixtures.secondarytable.shop.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/** Violation: the entity is prefixed but its secondary table is not. */
@Entity
@Table(name = "shop_thing")
@SecondaryTable(name = "thing_detail")
public class UnprefixedSecondaryTableEntity {

  @Id String id;
}
