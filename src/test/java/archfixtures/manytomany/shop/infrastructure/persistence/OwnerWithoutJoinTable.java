package archfixtures.manytomany.shop.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.List;

/** Violation: owning {@code @ManyToMany} without {@code @JoinTable} gets a default table name. */
@Entity
@Table(name = "shop_thing")
public class OwnerWithoutJoinTable {

  @Id String id;

  @ManyToMany List<TagEntity> tags;
}
