package archfixtures.manytomany.shop.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Target of the association; correctly prefixed on its own. */
@Entity
@Table(name = "shop_tag")
public class TagEntity {

  @Id String id;
}
