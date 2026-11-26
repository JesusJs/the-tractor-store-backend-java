package archfixtures.txoutsideapp.shop.infrastructure.web;

import org.springframework.transaction.annotation.Transactional;

/** Violation: transaction boundary outside the application layer. */
public class TransactionalController {

  @Transactional
  public void handle() {}
}
