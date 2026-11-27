package archfixtures.txclass.shop.infrastructure.web;

import org.springframework.transaction.annotation.Transactional;

/** Violation: class-level transaction boundary outside the application layer. */
@Transactional
public class TransactionalClassController {

  public void handle() {}
}
