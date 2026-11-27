package archfixtures.onetomany.shop.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Target of the association; correctly prefixed on its own. */
@Entity
@Table(name = "shop_item")
public class ItemEntity {

  @Id String id;
}
