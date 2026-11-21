package archfixtures.webport.shop.infrastructure.web;

import archfixtures.webport.shop.domain.port.out.ShopRepository;

/** Violation: controller injects an outbound port instead of a use case. */
public class ShopController {

  private final ShopRepository repository;

  public ShopController(ShopRepository repository) {
    this.repository = repository;
  }

  public String get() {
    return repository.find();
  }
}
