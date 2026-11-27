package archfixtures.elementcollection.shop.infrastructure.persistence;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;

/**
 * Violation: {@code @ElementCollection} without {@code @CollectionTable} gets an unprefixed table.
 */
@Entity
@Table(name = "shop_thing")
public class DefaultCollectionTableEntity {

  @Id String id;

  @ElementCollection List<String> tags;
}
