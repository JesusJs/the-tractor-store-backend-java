package archfixtures.txjakarta.shop.infrastructure.web;

import jakarta.transaction.Transactional;

/** Violation: JTA transaction boundary outside the application layer. */
public class JakartaTransactionalController {

  @Transactional
  public void handle() {}
}
