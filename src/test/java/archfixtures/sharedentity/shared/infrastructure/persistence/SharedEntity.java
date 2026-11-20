package archfixtures.sharedentity.shared.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Violation: the shared technical module owns a table. */
@Entity
@Table(name = "shared_thing")
public class SharedEntity {

  @Id String id;
}
