package archfixtures.onetomany.shop.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;

/**
 * Violation: unidirectional {@code @OneToMany} without {@code @JoinColumn} nor {@code @JoinTable}
 * gets a default join table named {@code <entity>_<field>}, without the module prefix.
 */
@Entity
@Table(name = "shop_owner")
public class OwnerWithoutJoinColumn {

  @Id String id;

  @OneToMany List<ItemEntity> items;
}
