package archfixtures.collectionprefix.shop.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;

/** Violation: the entity is prefixed but its collection table is not. */
@Entity
@Table(name = "shop_thing")
public class UnprefixedCollectionEntity {

  @Id String id;

  @ElementCollection
  @CollectionTable(name = "thing_tag")
  List<String> tags;
}
