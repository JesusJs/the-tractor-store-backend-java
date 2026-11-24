package archfixtures.repoplace.shop.application;

import org.springframework.data.repository.Repository;

/** Violation: Spring Data repository outside persistence. */
public interface MisplacedRepository extends Repository<Object, String> {}
